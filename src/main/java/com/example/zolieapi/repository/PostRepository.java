package com.example.zolieapi.repository;

import com.example.zolieapi.model.Post;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, UUID> {

    String VISIBLE = " from posts p join users a on a.id = p.author_id where p.deleted_at is null and "
            + VisibilitySql.CAN_SEE_POST;

    String PAGE = " and (p.created_at, p.id) < (:time, :id) order by p.created_at desc, p.id desc limit :limit ";

    @Query(value = "select p.*" + VISIBLE + PAGE, nativeQuery = true)
    List<Post> findFeed(UUID viewer, Instant time, UUID id, int limit);

    @Query(value = "select p.*" + VISIBLE + " and p.author_id = :author " + PAGE, nativeQuery = true)
    List<Post> findByAuthor(UUID author, UUID viewer, Instant time, UUID id, int limit);

    @Query(value = "select p.*" + VISIBLE + " and p.id = :postId", nativeQuery = true)
    Optional<Post> findVisible(UUID postId, UUID viewer);

    @Query(value = """
            select p.* from saved_posts s join posts p on p.id = s.post_id join users a on a.id = p.author_id
            where s.user_id = :viewer and p.deleted_at is null and
            """ + VisibilitySql.CAN_SEE_POST + PAGE, nativeQuery = true)
    List<Post> findSaved(UUID viewer, Instant time, UUID id, int limit);

    @Query(value = "select p.*" + VISIBLE + """
             and a.account_type = 'public' and p.visibility = 'public' and p.text ilike :pattern escape '\\'
            """ + PAGE, nativeQuery = true)
    List<Post> search(String pattern, UUID viewer, Instant time, UUID id, int limit);

    @Modifying
    @Query(value = "insert into post_likes (post_id, user_id) values (:postId, :userId) on conflict do nothing",
            nativeQuery = true)
    int insertLike(UUID postId, UUID userId);

    @Modifying
    @Query(value = "delete from post_likes where post_id = :postId and user_id = :userId", nativeQuery = true)
    int deleteLike(UUID postId, UUID userId);

    @Modifying
    @Query(value = "insert into saved_posts (user_id, post_id) values (:userId, :postId) on conflict do nothing",
            nativeQuery = true)
    int insertSave(UUID postId, UUID userId);

    @Modifying
    @Query(value = "delete from saved_posts where post_id = :postId and user_id = :userId", nativeQuery = true)
    int deleteSave(UUID postId, UUID userId);

    @Query(value = "select post_id from post_likes where user_id = :userId and post_id in (:postIds)", nativeQuery = true)
    List<UUID> findLikedIds(UUID userId, Collection<UUID> postIds);

    @Query(value = "select post_id from saved_posts where user_id = :userId and post_id in (:postIds)", nativeQuery = true)
    List<UUID> findSavedIds(UUID userId, Collection<UUID> postIds);

    @Modifying
    @Query("update Post p set p.likeCount = p.likeCount + :delta where p.id = :id")
    void addLikeCount(UUID id, int delta);

    @Modifying
    @Query("update Post p set p.commentCount = p.commentCount + :delta where p.id = :id")
    void addCommentCount(UUID id, int delta);

    @Modifying
    @Query("update Post p set p.shareCount = p.shareCount + 1 where p.id = :id")
    void addShare(UUID id);

    @Query("select p.likeCount from Post p where p.id = :id")
    int findLikeCount(UUID id);

    @Query("select p.shareCount from Post p where p.id = :id")
    int findShareCount(UUID id);

    @Modifying
    @Query(value = "delete from media_assets where id in (select media_id from post_media where post_id = :postId)",
            nativeQuery = true)
    void deleteMedia(UUID postId);
}
