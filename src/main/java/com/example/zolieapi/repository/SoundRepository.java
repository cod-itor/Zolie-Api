package com.example.zolieapi.repository;

import com.example.zolieapi.model.Sound;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SoundRepository extends JpaRepository<Sound, UUID> {

    @Query(value = """
            select s.* from sounds s
            where (cast(:pattern as text) is null or s.title ilike :pattern escape '\\' or s.artist_name ilike :pattern escape '\\')
              and (s.is_library or s.audio_media_id is not null or exists (
                  select 1 from reels r join users a on a.id = r.author_id
                  where r.sound_id = s.id and r.status = 'ready' and r.deleted_at is null
                    and r.visibility = 'public' and a.account_type = 'public' and a.status = 'active'))
            order by s.is_library desc, s.use_count desc, s.id desc limit :limit
            """, nativeQuery = true)
    List<Sound> search(String pattern, int limit);

    @Modifying
    @Query("update Sound s set s.useCount = s.useCount + 1 where s.id = :id")
    void addUse(UUID id);
}
