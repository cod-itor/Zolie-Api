package com.example.zolieapi.repository;

import com.example.zolieapi.model.Reel;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ReelRepository extends JpaRepository<Reel, UUID> {

    String VISIBLE = " from reels r join users a on a.id = r.author_id where r.deleted_at is null and "
            + VisibilitySql.CAN_SEE_REEL + " and (r.status = 'ready' or r.author_id = :viewer) ";

    String PAGE = " and (r.created_at, r.id) < (:time, :id) order by r.created_at desc, r.id desc limit :limit ";

    @Query(value = "select r.*" + VISIBLE + " and r.status = 'ready' " + PAGE, nativeQuery = true)
    List<Reel> findFeed(UUID viewer, Instant time, UUID id, int limit);

    @Query(value = "select r.*" + VISIBLE + " and r.status = 'ready' and " + VisibilitySql.FOLLOWS_AUTHOR + PAGE,
            nativeQuery = true)
    List<Reel> findFollowingFeed(UUID viewer, Instant time, UUID id, int limit);

    @Query(value = "select r.*" + VISIBLE + " and r.author_id = :author " + PAGE, nativeQuery = true)
    List<Reel> findByAuthor(UUID author, UUID viewer, Instant time, UUID id, int limit);

    @Query(value = "select exists (select 1 from reels r join users a on a.id = r.author_id "
            + "where r.id = :reelId and r.deleted_at is null and " + VisibilitySql.CAN_SEE_REEL + ")", nativeQuery = true)
    boolean isVisibleOnceReady(UUID reelId, UUID viewer);

    @Query(value = "select r.*" + VISIBLE + " and r.id = :reelId", nativeQuery = true)
    Optional<Reel> findVisible(UUID reelId, UUID viewer);

    @Query(value = """
            select r.* from saved_reels s join reels r on r.id = s.reel_id join users a on a.id = r.author_id
            where s.user_id = :viewer and r.deleted_at is null and r.status = 'ready' and
            """ + VisibilitySql.CAN_SEE_REEL + PAGE, nativeQuery = true)
    List<Reel> findSaved(UUID viewer, Instant time, UUID id, int limit);

    @Query(value = "select r.*" + VISIBLE + """
             and r.status = 'ready' and a.account_type = 'public' and r.visibility = 'public'
             and r.caption ilike :pattern escape '\\'
            """ + PAGE, nativeQuery = true)
    List<Reel> search(String pattern, UUID viewer, Instant time, UUID id, int limit);

    @Query(value = """
            select r.* from reels r where r.status = 'processing' and r.deleted_at is null
            order by r.created_at limit :limit for update skip locked
            """, nativeQuery = true)
    List<Reel> lockNextToProcess(int limit);

    @Query(value = "select r.*" + VISIBLE + " and r.status = 'ready' and r.sound_id = :soundId " + PAGE, nativeQuery = true)
    List<Reel> findBySound(UUID soundId, UUID viewer, Instant time, UUID id, int limit);

    @Query(value = "select r.*" + VISIBLE + """
             and r.status = 'ready' and a.account_type = 'public' and r.visibility = 'public'
             and exists (select 1 from reel_hashtags rh join hashtags h on h.id = rh.hashtag_id
                 where rh.reel_id = r.id and h.tag = :tag)
            """ + PAGE, nativeQuery = true)
    List<Reel> findByHashtag(String tag, UUID viewer, Instant time, UUID id, int limit);

    @Modifying
    @Query(value = """
            insert into reel_likes (reel_id, user_id, reaction) values (:reelId, :userId, :reaction)
            on conflict do nothing
            """, nativeQuery = true)
    int insertLike(UUID reelId, UUID userId, String reaction);

    @Modifying
    @Query(value = "update reel_likes set reaction = :reaction where reel_id = :reelId and user_id = :userId",
            nativeQuery = true)
    void updateReaction(UUID reelId, UUID userId, String reaction);

    @Query(value = """
            select reel_id as contentId, reaction as reaction from reel_likes
            where user_id = :userId and reel_id in (:reelIds)
            """, nativeQuery = true)
    List<PostRepository.ReactionRow> findMyReactions(UUID userId, Collection<UUID> reelIds);

    @Modifying
    @Query(value = "delete from reel_likes where reel_id = :reelId and user_id = :userId", nativeQuery = true)
    int deleteLike(UUID reelId, UUID userId);

    @Modifying
    @Query(value = "insert into saved_reels (user_id, reel_id) values (:userId, :reelId) on conflict do nothing",
            nativeQuery = true)
    int insertSave(UUID reelId, UUID userId);

    @Modifying
    @Query(value = "delete from saved_reels where reel_id = :reelId and user_id = :userId", nativeQuery = true)
    int deleteSave(UUID reelId, UUID userId);

    @Query(value = "select reel_id from saved_reels where user_id = :userId and reel_id in (:reelIds)", nativeQuery = true)
    List<UUID> findSavedIds(UUID userId, Collection<UUID> reelIds);

    @Modifying
    @Query("update Reel r set r.likeCount = r.likeCount + :delta where r.id = :id")
    void addLikeCount(UUID id, int delta);

    @Modifying
    @Query("update Reel r set r.commentCount = r.commentCount + :delta where r.id = :id")
    void addCommentCount(UUID id, int delta);

    @Modifying
    @Query("update Reel r set r.shareCount = r.shareCount + 1 where r.id = :id")
    void addShare(UUID id);

    @Modifying
    @Query("update Reel r set r.viewCount = r.viewCount + 1 where r.id = :id")
    void addView(UUID id);

    @Query("select r.likeCount from Reel r where r.id = :id")
    int findLikeCount(UUID id);

    @Query("select r.shareCount from Reel r where r.id = :id")
    int findShareCount(UUID id);
}
