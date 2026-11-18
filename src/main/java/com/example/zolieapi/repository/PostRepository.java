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

    String NOT_HIDDEN = " and not exists (select 1 from hidden_posts h where h.user_id = :viewer and h.post_id = p.id) ";

    @Query(value = "select p.*" + VISIBLE + NOT_HIDDEN + PAGE, nativeQuery = true)
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

    @Query(value = "select p.*" + VISIBLE + " and p.author_id = :author and p.pinned_at is not null "
            + "order by p.pinned_at desc limit 3", nativeQuery = true)
    List<Post> findPinned(UUID author, UUID viewer);

    @Query(value = "select count(*) from posts where author_id = :author and pinned_at is not null and deleted_at is null",
            nativeQuery = true)
    long countPinned(UUID author);

    @Query(value = "select p.*" + VISIBLE + """
             and a.account_type = 'public' and p.visibility = 'public'
             and exists (select 1 from post_hashtags ph join hashtags h on h.id = ph.hashtag_id
                 where ph.post_id = p.id and h.tag = :tag)
            """ + PAGE, nativeQuery = true)
    List<Post> findByHashtag(String tag, UUID viewer, Instant time, UUID id, int limit);

    @Modifying
    @Query(value = """
            insert into post_likes (post_id, user_id, reaction) values (:postId, :userId, :reaction)
            on conflict do nothing
            """, nativeQuery = true)
    int insertLike(UUID postId, UUID userId, String reaction);

    @Modifying
    @Query(value = "update post_likes set reaction = :reaction where post_id = :postId and user_id = :userId",
            nativeQuery = true)
    void updateReaction(UUID postId, UUID userId, String reaction);

    @Query(value = """
            select post_id as contentId, reaction as reaction from post_likes
            where user_id = :userId and post_id in (:postIds)
            """, nativeQuery = true)
    List<ReactionRow> findMyReactions(UUID userId, Collection<UUID> postIds);

    @Query(value = """
            select post_id as contentId, reaction as reaction, count(*) as total from post_likes
            where post_id in (:postIds) group by post_id, reaction order by post_id, count(*) desc
            """, nativeQuery = true)
    List<ReactionCountRow> countReactions(Collection<UUID> postIds);

    @Modifying
    @Query(value = "insert into hidden_posts (user_id, post_id) values (:userId, :postId) on conflict do nothing",
            nativeQuery = true)
    void hide(UUID postId, UUID userId);

    @Modifying
    @Query(value = "delete from hidden_posts where user_id = :userId and post_id = :postId", nativeQuery = true)
    void unhide(UUID postId, UUID userId);

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

    interface ReactionRow {

        UUID getContentId();

        String getReaction();
    }

    interface ReactionCountRow {

        UUID getContentId();

        String getReaction();

        long getTotal();
    }

    @Modifying
    @Query(value = "delete from media_assets where id in (select media_id from post_media where post_id = :postId)",
            nativeQuery = true)
    void deleteMedia(UUID postId);
}
