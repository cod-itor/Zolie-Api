package com.example.zolieapi.repository;

import com.example.zolieapi.model.StoryStickerResponse;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface StoryStickerResponseRepository extends JpaRepository<StoryStickerResponse, UUID> {

    Optional<StoryStickerResponse> findByStickerIdAndResponderId(UUID stickerId, UUID responderId);

    List<StoryStickerResponse> findByResponderIdAndStickerIdIn(UUID responderId, Collection<UUID> stickerIds);

    List<StoryStickerResponse> findByStickerIdAndRemindMeTrue(UUID stickerId);

    @Query(value = """
            select r.* from story_sticker_responses r
            where r.sticker_id = :stickerId and not r.remind_me and (r.created_at, r.id) < (:time, :id)
            order by r.created_at desc, r.id desc limit :limit
            """, nativeQuery = true)
    List<StoryStickerResponse> findAnswers(UUID stickerId, Instant time, UUID id, int limit);

    @Query("select avg(r.sliderValue) from StoryStickerResponse r where r.stickerId = :stickerId and r.sliderValue is not null")
    Double averageSlider(UUID stickerId);

    @Modifying
    @Query("delete from StoryStickerResponse r where r.stickerId = :stickerId and r.remindMe = true")
    void deleteReminders(UUID stickerId);
}
