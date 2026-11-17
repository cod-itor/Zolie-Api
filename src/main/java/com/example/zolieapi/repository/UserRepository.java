package com.example.zolieapi.repository;

import com.example.zolieapi.model.User;
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
