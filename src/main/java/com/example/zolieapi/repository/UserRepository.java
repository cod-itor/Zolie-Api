package com.example.zolieapi.repository;

import com.example.zolieapi.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

    String PAGE = " and a.id < :after order by a.id desc limit :limit ";

    Optional<User> findByFirebaseUid(String firebaseUid);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    @Modifying
    @Query("update User u set u.followerCount = u.followerCount + :delta where u.id = :id")
    void addFollowerCount(UUID id, int delta);

    @Modifying
    @Query("update User u set u.followingCount = u.followingCount + :delta where u.id = :id")
    void addFollowingCount(UUID id, int delta);

    @Modifying
    @Query("update User u set u.postCount = u.postCount + :delta where u.id = :id")
    void addPostCount(UUID id, int delta);

    @Modifying
    @Query("update User u set u.lastActiveAt = current_timestamp where u.id = :id")
    void touchLastActive(UUID id);

    @Query(value = "select exists (select 1 from users a where a.id = :author and " + VisibilitySql.CAN_SEE_AUTHOR + ")",
            nativeQuery = true)
    boolean canSeeContent(UUID viewer, UUID author);

    @Query(value = """
            select a.* from follows f join users a on a.id = f.follower_id
            where f.followed_id = :userId and f.status = 'accepted' and
            """ + VisibilitySql.AUTHOR_ACTIVE + " and " + VisibilitySql.NOT_BLOCKED + PAGE, nativeQuery = true)
    List<User> findFollowers(UUID userId, UUID viewer, UUID after, int limit);

    @Query(value = """
            select a.* from follows f join users a on a.id = f.followed_id
            where f.follower_id = :userId and f.status = 'accepted' and
            """ + VisibilitySql.AUTHOR_ACTIVE + " and " + VisibilitySql.NOT_BLOCKED + PAGE, nativeQuery = true)
    List<User> findFollowing(UUID userId, UUID viewer, UUID after, int limit);

    @Query(value = """
            select a.* from follows f join users a on a.id = f.follower_id
            where f.followed_id = :userId and f.status = 'pending' and
            """ + VisibilitySql.AUTHOR_ACTIVE + PAGE, nativeQuery = true)
    List<User> findFollowRequesters(UUID userId, UUID after, int limit);

    @Query(value = """
            select a.* from post_likes l join users a on a.id = l.user_id
            where l.post_id = :postId and
            """ + VisibilitySql.AUTHOR_ACTIVE + " and " + VisibilitySql.NOT_BLOCKED + PAGE, nativeQuery = true)
    List<User> findPostLikers(UUID postId, UUID viewer, UUID after, int limit);

    @Query(value = """
            select a.* from story_views v join users a on a.id = v.viewer_id
            where v.story_id = :storyId and
            """ + VisibilitySql.AUTHOR_ACTIVE + PAGE, nativeQuery = true)
    List<User> findStoryViewers(UUID storyId, UUID after, int limit);

    @Query(value = """
            select a.* from users a
            where (a.username ilike :pattern escape '\\' or a.display_name ilike :pattern escape '\\') and
            """ + VisibilitySql.AUTHOR_ACTIVE + " and " + VisibilitySql.NOT_BLOCKED + PAGE, nativeQuery = true)
    List<User> search(String pattern, UUID viewer, UUID after, int limit);

    @Modifying
    @Query(value = """
            update users set status = 'active', suspended_until = null
            where status = 'suspended' and suspended_until is not null and suspended_until <= now()
            """, nativeQuery = true)
    int liftEndedSuspensions();

    @Modifying
    @Query(value = """
            update users set follower_count = follower_count - 1
            where id in (select followed_id from follows where follower_id = :userId and status = 'accepted')
            """, nativeQuery = true)
    void releaseFollowerCounts(UUID userId);

    @Modifying
    @Query(value = """
            update users set following_count = following_count - 1
            where id in (select follower_id from follows where followed_id = :userId and status = 'accepted')
            """, nativeQuery = true)
    void releaseFollowingCounts(UUID userId);

    @Modifying
    @Query(value = """
            update posts set like_count = like_count - 1
            where id in (select post_id from post_likes where user_id = :userId)
            """, nativeQuery = true)
    void releasePostLikeCounts(UUID userId);

    @Modifying
    @Query(value = """
            update reels set like_count = like_count - 1
            where id in (select reel_id from reel_likes where user_id = :userId)
            """, nativeQuery = true)
    void releaseReelLikeCounts(UUID userId);

    @Modifying
    @Query(value = """
            update poll_options set vote_count = vote_count - 1
            where id in (select option_id from poll_votes where user_id = :userId)
            """, nativeQuery = true)
    void releasePollVoteCounts(UUID userId);

    @Modifying
    @Query(value = """
            update posts p set comment_count = p.comment_count - gone.total
            from (select c.post_id, count(*) as total from comments c
                  left join comments parent on parent.id = c.parent_id
                  where c.post_id is not null and c.deleted_at is null
                    and (c.author_id = :userId or parent.author_id = :userId)
                  group by c.post_id) gone
            where p.id = gone.post_id
            """, nativeQuery = true)
    void releasePostCommentCounts(UUID userId);

    @Modifying
    @Query(value = """
            update reels r set comment_count = r.comment_count - gone.total
            from (select c.reel_id, count(*) as total from comments c
                  left join comments parent on parent.id = c.parent_id
                  where c.reel_id is not null and c.deleted_at is null
                    and (c.author_id = :userId or parent.author_id = :userId)
                  group by c.reel_id) gone
            where r.id = gone.reel_id
            """, nativeQuery = true)
    void releaseReelCommentCounts(UUID userId);

    List<User> findByUsernameIn(Collection<String> usernames);

    @Query(value = """
            select a.* from blocks b join users a on a.id = b.blocked_id
            where b.blocker_id = :userId
            """ + PAGE, nativeQuery = true)
    List<User> findBlockedBy(UUID userId, UUID after, int limit);

    @Query(value = """
            select a.* from close_friends c join users a on a.id = c.friend_id
            where c.owner_id = :userId and
            """ + VisibilitySql.AUTHOR_ACTIVE + PAGE, nativeQuery = true)
    List<User> findCloseFriendsOf(UUID userId, UUID after, int limit);
}
