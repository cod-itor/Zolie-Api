package com.example.zolieapi.repository;

import com.example.zolieapi.model.SavedCollection;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CollectionRepository extends JpaRepository<SavedCollection, UUID> {

    List<SavedCollection> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<SavedCollection> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);

    long countByUserId(UUID userId);

    @Query(value = """
            select collection_id as collectionId, count(*) as total from collection_items
            where collection_id in (:collectionIds) group by collection_id
            """, nativeQuery = true)
    List<ItemCountRow> countItems(Collection<UUID> collectionIds);

    interface ItemCountRow {

        UUID getCollectionId();

        long getTotal();
    }
}
