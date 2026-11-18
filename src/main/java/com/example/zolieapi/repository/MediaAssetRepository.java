package com.example.zolieapi.repository;

import com.example.zolieapi.model.MediaAsset;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    @Query(value = """
            select exists (select 1 from post_media where media_id = :mediaId)
                or exists (select 1 from reels where video_id = :mediaId)
                or exists (select 1 from stories where media_id = :mediaId)
            """, nativeQuery = true)
    boolean isAttached(UUID mediaId);

    @Modifying
    @Query(value = "delete from media_assets where status = 'uploading' and created_at < now() - interval '24 hours'",
            nativeQuery = true)
    int deleteAbandonedUploads();

    @Query(value = "select id, object_key as objectKey from media_deletion_queue order by queued_at limit :limit",
            nativeQuery = true)
    List<DeletionRow> findQueuedDeletions(int limit);

    @Modifying
    @Query(value = "delete from media_deletion_queue where id = :id", nativeQuery = true)
    void removeQueuedDeletion(long id);

    @Modifying
    @Query(value = """
            update media_deletion_queue set attempts = attempts + 1, last_error = :error, queued_at = now()
            where id = :id
            """, nativeQuery = true)
    void markDeletionFailed(long id, String error);

    @Query(value = "select pg_try_advisory_xact_lock(hashtextextended(:name, 0))", nativeQuery = true)
    boolean tryLockJob(String name);

    interface DeletionRow {

        Long getId();

        String getObjectKey();
    }
}
