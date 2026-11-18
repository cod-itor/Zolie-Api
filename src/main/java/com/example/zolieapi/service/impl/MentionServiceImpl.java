package com.example.zolieapi.service.impl;

import com.example.zolieapi.model.Mention;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.AccountStatus;
import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.repository.CommentRepository;
import com.example.zolieapi.repository.MentionRepository;
import com.example.zolieapi.repository.PostRepository;
import com.example.zolieapi.repository.ReelRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.MentionService;
import com.example.zolieapi.service.NotificationService;
import com.example.zolieapi.service.VisibilityService;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MentionServiceImpl implements MentionService {

    private static final Pattern USERNAME_MENTION = Pattern.compile("@([a-z0-9._]{3,30})");
    private static final int MAX_MENTIONS = 20;

    private final MentionRepository mentionRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReelRepository reelRepository;
    private final CommentRepository commentRepository;
    private final VisibilityService visibilityService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void mention(UUID authorId, String text, Collection<UUID> taggedUserIds, NotificationTarget source,
                        UUID sourceId) {
        resolve(authorId, text, taggedUserIds, source, sourceId).forEach(userId -> save(authorId, userId, source, sourceId));
    }

    @Override
    @Transactional
    public void replaceForPost(UUID authorId, UUID postId, String text, Collection<UUID> taggedUserIds) {
        Set<UUID> wanted = resolve(authorId, text, taggedUserIds, NotificationTarget.POST, postId);
        List<Mention> existing = mentionRepository.findByPostId(postId);
        Set<UUID> current = existing.stream().map(Mention::getMentionedUserId).collect(Collectors.toSet());

        mentionRepository.deleteAll(existing.stream()
                .filter(mention -> !wanted.contains(mention.getMentionedUserId()))
                .toList());
        wanted.stream()
                .filter(userId -> !current.contains(userId))
                .forEach(userId -> save(authorId, userId, NotificationTarget.POST, postId));
    }

    private Set<UUID> resolve(UUID authorId, String text, Collection<UUID> taggedUserIds, NotificationTarget source,
                              UUID sourceId) {
        Set<String> usernames = new LinkedHashSet<>();
        if (text != null) {
            Matcher matcher = USERNAME_MENTION.matcher(text.toLowerCase());
            while (matcher.find()) {
                usernames.add(matcher.group(1));
            }
        }
        Set<User> users = new LinkedHashSet<>(userRepository.findByUsernameIn(usernames));
        if (taggedUserIds != null && !taggedUserIds.isEmpty()) {
            users.addAll(userRepository.findAllById(taggedUserIds.stream().distinct().limit(MAX_MENTIONS).toList()));
        }
        return users.stream()
                .filter(user -> user.getStatus() == AccountStatus.ACTIVE)
                .filter(user -> visibilityService.allows(user.getWhoCanMention(), user.getId(), authorId))
                .map(User::getId)
                .filter(userId -> !userId.equals(authorId))
                .limit(MAX_MENTIONS)
                .filter(userId -> !visibilityService.isBlockedBetween(authorId, userId))
                .filter(userId -> canSee(userId, source, sourceId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean canSee(UUID userId, NotificationTarget source, UUID sourceId) {
        return switch (source) {
            case POST -> postRepository.findVisible(sourceId, userId).isPresent();
            case REEL -> reelRepository.isVisibleOnceReady(sourceId, userId);
            case COMMENT -> commentRepository.findById(sourceId)
                    .map(comment -> comment.getPostId() != null
                            ? postRepository.findVisible(comment.getPostId(), userId).isPresent()
                            : reelRepository.isVisibleOnceReady(comment.getReelId(), userId))
                    .orElse(false);
            default -> false;
        };
    }

    private void save(UUID authorId, UUID mentionedUserId, NotificationTarget source, UUID sourceId) {
        Mention mention = Mention.builder().authorId(authorId).mentionedUserId(mentionedUserId).build();
        switch (source) {
            case POST -> mention.setPostId(sourceId);
            case REEL -> mention.setReelId(sourceId);
            case COMMENT -> mention.setCommentId(sourceId);
            default -> throw new IllegalArgumentException("Unsupported mention source: " + source);
        }
        mentionRepository.save(mention);
        notificationService.send(mentionedUserId, authorId, NotificationType.MENTION, source, sourceId);
    }
}
