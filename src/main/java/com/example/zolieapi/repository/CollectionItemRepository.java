package com.example.zolieapi.repository;

import com.example.zolieapi.model.CollectionItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CollectionItemRepository extends JpaRepository<CollectionItem, UUID> {

    boolean existsByCollectionIdAndPostId(UUID collectionId, UUID postId);

    boolean existsByCollectionIdAndReelId(UUID collectionId, UUID reelId);

    @Modifying
    @Query("delete from CollectionItem i where i.collectionId = :collectionId and i.postId = :postId")
    void removePost(UUID collectionId, UUID postId);

    @Modifying
    @Query("delete from CollectionItem i where i.collectionId = :collectionId and i.reelId = :reelId")
    void removeReel(UUID collectionId, UUID reelId);

    @Query(value = """
            select i.* from collection_items i
            where i.collection_id = :collectionId and (i.added_at, i.id) < (:time, :id)
            order by i.added_at desc, i.id desc limit :limit
            """, nativeQuery = true)
    List<CollectionItem> findPage(UUID collectionId, Instant time, UUID id, int limit);
}
