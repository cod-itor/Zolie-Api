package com.example.zolieapi.repository;

import com.example.zolieapi.model.StorySticker;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface StoryStickerRepository extends JpaRepository<StorySticker, UUID> {

    List<StorySticker> findByStoryIdInOrderByCreatedAtAsc(Collection<UUID> storyIds);

    @Modifying
    @Query("update StorySticker s set s.responseCount = s.responseCount + 1 where s.id = :id")
    void addResponse(UUID id);

    @Query(value = """
            select k.* from story_stickers k
            where k.type = 'countdown' and k.countdown_ends_at <= now()
              and exists (select 1 from story_sticker_responses r where r.sticker_id = k.id and r.remind_me)
            limit :limit
            """, nativeQuery = true)
    List<StorySticker> findEndedCountdownsWithReminders(int limit);
}
