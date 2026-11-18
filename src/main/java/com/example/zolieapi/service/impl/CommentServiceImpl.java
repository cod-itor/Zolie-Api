package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.request.CommentRequest;
import com.example.zolieapi.dto.response.CommentResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.mapper.UserMapper;
import com.example.zolieapi.dto.response.LikeResponse;
import com.example.zolieapi.model.Comment;
import com.example.zolieapi.model.Post;
import com.example.zolieapi.model.Reel;
import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.repository.CommentRepository;
import com.example.zolieapi.repository.CommentRepository.ReplyCountRow;
import com.example.zolieapi.repository.PostRepository;
import com.example.zolieapi.repository.ReelRepository;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.service.CommentService;
import com.example.zolieapi.service.MentionService;
import com.example.zolieapi.service.NotificationService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final int MAX_TEXT = 1000;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ReelRepository reelRepository;
    private final MentionService mentionService;
    private final NotificationService notificationService;
    private final RateLimiter rateLimiter;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPage<CommentResponse> listForPost(UUID viewerId, UUID postId, String cursor, Integer limit) {
        UUID contentAuthorId = requirePostAuthor(viewerId, postId);
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return page(viewerId, contentAuthorId,
                commentRepository.findForPost(postId, viewerId, from.time(), from.id(), size + 1), size);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<CommentResponse> listForReel(UUID viewerId, UUID reelId, String cursor, Integer limit) {
        UUID contentAuthorId = requireReelAuthor(viewerId, reelId);
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return page(viewerId, contentAuthorId,
                commentRepository.findForReel(reelId, viewerId, from.time(), from.id(), size + 1), size);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<CommentResponse> listReplies(UUID viewerId, UUID commentId, String cursor, Integer limit) {
        Comment parent = requireComment(commentId);
        UUID contentAuthorId = requireContentAuthor(viewerId, parent);
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decodeAscending(cursor);
        return page(viewerId, contentAuthorId,
                commentRepository.findReplies(commentId, viewerId, from.time(), from.id(), size + 1), size);
    }

    @Override
    @Transactional
    public CommentResponse addToPost(UUID userId, UUID postId, CommentRequest request) {
        Post post = postRepository.findVisible(postId, userId).orElseThrow(ApiException::notFound);
        requireCommentsEnabled(post.isCommentsEnabled());
        UUID contentAuthorId = post.getAuthorId();
        Comment comment = save(userId, Comment.builder().postId(postId), request);
        postRepository.addCommentCount(postId, 1);
        notifyAndMention(userId, contentAuthorId, comment, NotificationTarget.POST, postId);
        return toResponses(userId, contentAuthorId, List.of(comment)).get(0);
    }

    @Override
    @Transactional
    public CommentResponse addToReel(UUID userId, UUID reelId, CommentRequest request) {
        Reel reel = reelRepository.findVisible(reelId, userId).orElseThrow(ApiException::notFound);
        requireCommentsEnabled(reel.isCommentsEnabled());
        UUID contentAuthorId = reel.getAuthorId();
        Comment comment = save(userId, Comment.builder().reelId(reelId), request);
        reelRepository.addCommentCount(reelId, 1);
        notifyAndMention(userId, contentAuthorId, comment, NotificationTarget.REEL, reelId);
        return toResponses(userId, contentAuthorId, List.of(comment)).get(0);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID commentId) {
        Comment comment = requireComment(commentId);
        if (!comment.getAuthorId().equals(userId) && !requireContentAuthor(userId, comment).equals(userId)) {
            throw ApiException.forbidden();
        }
        int removed = commentRepository.softDeleteWithReplies(commentId);
        if (comment.getPostId() != null) {
            postRepository.addCommentCount(comment.getPostId(), -removed);
        } else {
            reelRepository.addCommentCount(comment.getReelId(), -removed);
        }
    }

    @Override
    @Transactional
    public LikeResponse like(UUID userId, UUID commentId) {
        rateLimiter.check("like", userId, 120, Duration.ofMinutes(1));
        Comment comment = requireComment(commentId);
        requireContentAuthor(userId, comment);
        if (commentRepository.insertLike(commentId, userId) == 1) {
            commentRepository.addLikeCount(commentId, 1);
            notificationService.send(comment.getAuthorId(), userId, NotificationType.COMMENT_LIKE,
                    NotificationTarget.COMMENT, commentId);
        }
        return new LikeResponse(true, null, commentRepository.findLikeCount(commentId));
    }

    @Override
    @Transactional
    public LikeResponse unlike(UUID userId, UUID commentId) {
        requireComment(commentId);
        if (commentRepository.deleteLike(commentId, userId) == 1) {
            commentRepository.addLikeCount(commentId, -1);
        }
        return new LikeResponse(false, null, commentRepository.findLikeCount(commentId));
    }

    private void requireCommentsEnabled(boolean enabled) {
        if (!enabled) {
            throw ApiException.rule("Comments are turned off.");
        }
    }

    private Comment save(UUID userId, Comment.CommentBuilder builder, CommentRequest request) {
        rateLimiter.check("comment", userId, 30, Duration.ofMinutes(1));
        Comment draft = builder.authorId(userId).text(Texts.required(request.text(), MAX_TEXT, "text")).build();
        if (request.parentId() != null) {
            draft.setParentId(resolveParent(draft, request.parentId()));
        }
        return commentRepository.save(draft);
    }

    private UUID resolveParent(Comment draft, UUID parentId) {
        Comment parent = requireComment(parentId);
        boolean sameContent = Objects.equals(parent.getPostId(), draft.getPostId())
                && Objects.equals(parent.getReelId(), draft.getReelId());
        if (!sameContent) {
            throw ApiException.rule("The parent comment belongs to different content.");
        }
        return parent.getParentId() != null ? parent.getParentId() : parent.getId();
    }

    private void notifyAndMention(UUID userId, UUID contentAuthorId, Comment comment, NotificationTarget target,
                                  UUID targetId) {
        notificationService.send(contentAuthorId, userId, NotificationType.COMMENT, target, targetId);
        if (comment.getParentId() != null) {
            UUID parentAuthorId = requireComment(comment.getParentId()).getAuthorId();
            if (!parentAuthorId.equals(contentAuthorId)) {
                notificationService.send(parentAuthorId, userId, NotificationType.COMMENT, target, targetId);
            }
        }
        commentRepository.flush();
        mentionService.mention(userId, comment.getText(), null, NotificationTarget.COMMENT, comment.getId());
    }

    private UUID requireContentAuthor(UUID viewerId, Comment comment) {
        return comment.getPostId() != null
                ? requirePostAuthor(viewerId, comment.getPostId())
                : requireReelAuthor(viewerId, comment.getReelId());
    }

    private UUID requirePostAuthor(UUID viewerId, UUID postId) {
        return postRepository.findVisible(postId, viewerId).orElseThrow(ApiException::notFound).getAuthorId();
    }

    private UUID requireReelAuthor(UUID viewerId, UUID reelId) {
        return reelRepository.findVisible(reelId, viewerId).orElseThrow(ApiException::notFound).getAuthorId();
    }

    private Comment requireComment(UUID commentId) {
        return commentRepository.findById(commentId)
                .filter(comment -> comment.getDeletedAt() == null)
                .orElseThrow(ApiException::notFound);
    }

    private CursorPage<CommentResponse> page(UUID viewerId, UUID contentAuthorId, List<Comment> comments, int size) {
        return CursorPage.of(comments, size, comment -> new Cursor(comment.getCreatedAt(), comment.getId()),
                page -> toResponses(viewerId, contentAuthorId, page));
    }

    private List<CommentResponse> toResponses(UUID viewerId, UUID contentAuthorId, List<Comment> comments) {
        if (comments.isEmpty()) {
            return List.of();
        }
        List<UUID> commentIds = comments.stream().map(Comment::getId).toList();
        Map<UUID, UserSummaryResponse> authors = userMapper.summariesById(comments.stream().map(Comment::getAuthorId).toList());
        Map<UUID, Long> replyCounts = commentRepository.countReplies(commentIds).stream()
                .collect(Collectors.toMap(ReplyCountRow::getParentId, ReplyCountRow::getTotal));
        Set<UUID> liked = new HashSet<>(commentRepository.findLikedIds(viewerId, commentIds));
        return comments.stream()
                .map(comment -> new CommentResponse(
                        comment.getId(),
                        authors.get(comment.getAuthorId()),
                        comment.getText(),
                        comment.getParentId(),
                        replyCounts.getOrDefault(comment.getId(), 0L),
                        comment.getLikeCount(),
                        liked.contains(comment.getId()),
                        comment.getAuthorId().equals(viewerId),
                        comment.getAuthorId().equals(contentAuthorId),
                        comment.getCreatedAt()))
                .toList();
    }
}
