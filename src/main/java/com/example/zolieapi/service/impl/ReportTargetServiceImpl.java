package com.example.zolieapi.service.impl;

import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.model.Comment;
import com.example.zolieapi.model.Message;
import com.example.zolieapi.model.Post;
import com.example.zolieapi.model.Reel;
import com.example.zolieapi.model.Story;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.ReportTarget;
import com.example.zolieapi.model.id.ParticipantId;
import com.example.zolieapi.repository.CommentRepository;
import com.example.zolieapi.repository.ConversationParticipantRepository;
import com.example.zolieapi.repository.MessageRepository;
import com.example.zolieapi.repository.PostRepository;
import com.example.zolieapi.repository.ReelRepository;
import com.example.zolieapi.repository.StoryRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.ReportTargetService;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportTargetServiceImpl implements ReportTargetService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReelRepository reelRepository;
    private final CommentRepository commentRepository;
    private final StoryRepository storyRepository;
    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MediaService mediaService;

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findOwner(ReportTarget targetType, UUID targetId) {
        return switch (targetType) {
            case USER -> userRepository.findById(targetId).map(User::getId);
            case POST -> postRepository.findById(targetId).map(Post::getAuthorId);
            case REEL -> reelRepository.findById(targetId).map(Reel::getAuthorId);
            case COMMENT -> commentRepository.findById(targetId).map(Comment::getAuthorId);
            case STORY -> storyRepository.findById(targetId).map(Story::getAuthorId);
            case MESSAGE -> messageRepository.findById(targetId).map(Message::getSenderId);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canReport(UUID reporterId, ReportTarget targetType, UUID targetId) {
        return switch (targetType) {
            case USER -> userRepository.existsById(targetId);
            case POST -> postRepository.findVisible(targetId, reporterId).isPresent();
            case REEL -> reelRepository.findVisible(targetId, reporterId).isPresent();
            case STORY -> storyRepository.findVisible(targetId, reporterId).isPresent();
            case COMMENT -> commentRepository.findById(targetId).filter(comment -> comment.getDeletedAt() == null)
                    .map(comment -> comment.getPostId() != null
                            ? postRepository.findVisible(comment.getPostId(), reporterId).isPresent()
                            : reelRepository.findVisible(comment.getReelId(), reporterId).isPresent())
                    .orElse(false);
            case MESSAGE -> messageRepository.findById(targetId)
                    .map(message -> participantRepository.existsById(
                            new ParticipantId(message.getConversationId(), reporterId)))
                    .orElse(false);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public String preview(ReportTarget targetType, UUID targetId) {
        return switch (targetType) {
            case USER -> userRepository.findById(targetId).map(user -> "@" + user.getUsername()).orElse(null);
            case POST -> postRepository.findById(targetId).map(Post::getText).orElse(null);
            case REEL -> reelRepository.findById(targetId).map(Reel::getCaption).orElse(null);
            case COMMENT -> commentRepository.findById(targetId).map(Comment::getText).orElse(null);
            case MESSAGE -> messageRepository.findById(targetId).map(Message::getText).orElse(null);
            case STORY -> null;
        };
    }

    @Override
    @Transactional
    public void remove(ReportTarget targetType, UUID targetId) {
        switch (targetType) {
            case USER -> throw ApiException.rule("An account cannot be removed. Suspend it instead.");
            case POST -> postRepository.findById(targetId).filter(post -> post.getDeletedAt() == null).ifPresent(this::removePost);
            case REEL -> reelRepository.findById(targetId).ifPresent(this::removeReel);
            case COMMENT -> commentRepository.findById(targetId).filter(comment -> comment.getDeletedAt() == null)
                    .ifPresent(this::removeComment);
            case STORY -> storyRepository.findById(targetId).ifPresent(story -> {
                storyRepository.delete(story);
                storyRepository.flush();
                mediaService.deleteAll(Arrays.asList(story.getMediaId()));
            });
            case MESSAGE -> messageRepository.findById(targetId).ifPresent(message -> message.setDeletedAt(Instant.now()));
        }
    }

    private void removePost(Post post) {
        post.setDeletedAt(Instant.now());
        postRepository.saveAndFlush(post);
        postRepository.deleteMedia(post.getId());
        userRepository.addPostCount(post.getAuthorId(), -1);
    }

    private void removeReel(Reel reel) {
        reelRepository.delete(reel);
        reelRepository.flush();
        mediaService.deleteAll(Arrays.asList(reel.getVideoId(), reel.getThumbnailId()));
    }

    private void removeComment(Comment comment) {
        int removed = commentRepository.softDeleteWithReplies(comment.getId());
        if (comment.getPostId() != null) {
            postRepository.addCommentCount(comment.getPostId(), -removed);
        } else {
            reelRepository.addCommentCount(comment.getReelId(), -removed);
        }
    }
}
