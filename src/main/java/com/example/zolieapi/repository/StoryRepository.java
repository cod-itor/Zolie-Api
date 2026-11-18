package com.example.zolieapi.repository;

import com.example.zolieapi.model.Story;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface StoryRepository extends JpaRepository<Story, UUID> {

    String VISIBLE = " from stories s join users a on a.id = s.author_id where " + VisibilitySql.CAN_SEE_STORY;

    @Query(value = "select s.*" + VISIBLE + " and s.author_id = :author order by s.created_at, s.id", nativeQuery = true)
    List<Story> findActiveByAuthor(UUID author, UUID viewer);

    @Query(value = "select s.*" + VISIBLE + " and s.id = :storyId", nativeQuery = true)
    Optional<Story> findVisible(UUID storyId, UUID viewer);

    @Query(value = """
            select s.author_id as userId, count(*) as storyCount,
                   bool_or(v.story_id is null) as hasUnseen,
                   bool_or(s.audience = 'close_friends') as closeFriends
            from stories s
            join users a on a.id = s.author_id
            left join story_views v on v.story_id = s.id and v.viewer_id = :viewer
            where
            """ + VisibilitySql.CAN_SEE_STORY + " and (a.id = :viewer or " + VisibilitySql.FOLLOWS_AUTHOR + ")" + """
            group by s.author_id
            order by (s.author_id = :viewer) desc, bool_or(v.story_id is null) desc, max(s.created_at) desc
            limit 100
            """, nativeQuery = true)
    List<RingRow> findRings(UUID viewer);

    @Query(value = "select story_id from story_views where viewer_id = :viewer and story_id in (:storyIds)",
            nativeQuery = true)
    List<UUID> findSeenIds(UUID viewer, Collection<UUID> storyIds);

    @Modifying
    @Query(value = "insert into story_views (story_id, viewer_id) values (:storyId, :viewer) on conflict do nothing",
            nativeQuery = true)
    int insertView(UUID storyId, UUID viewer);

    @Modifying
    @Query("update Story s set s.viewCount = s.viewCount + 1 where s.id = :id")
    void addView(UUID id);

    @Query(value = "select s.* from stories s where s.expires_at <= now() limit :limit", nativeQuery = true)
    List<Story> findExpired(int limit);

    interface RingRow {

        UUID getUserId();

        long getStoryCount();

        Boolean getHasUnseen();

        Boolean getCloseFriends();
    }
}
