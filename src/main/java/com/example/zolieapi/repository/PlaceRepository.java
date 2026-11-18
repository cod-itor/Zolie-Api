package com.example.zolieapi.repository;

import com.example.zolieapi.model.Place;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlaceRepository extends JpaRepository<Place, UUID> {

    List<Place> findTop20ByPopularTrueOrderByNameEnAsc();

    @Query(value = """
            select p.* from places p
            where p.name_en ilike :pattern escape '\\' or p.name_km ilike :pattern escape '\\'
            order by p.is_popular desc, p.name_en limit :limit
            """, nativeQuery = true)
    List<Place> search(String pattern, int limit);

    @Query(value = """
            select f.user_id as userId, p.id as id, p.type as type, p.name_en as nameEn, p.name_km as nameKm,
                   p.is_popular as popular
            from user_favorite_places f join places p on p.id = f.place_id
            where f.user_id in (:userIds) order by f.user_id, f.position
            """, nativeQuery = true)
    List<FavoriteRow> findFavorites(Collection<UUID> userIds);

    @Modifying
    @Query(value = "delete from user_favorite_places where user_id = :userId", nativeQuery = true)
    void deleteFavorites(UUID userId);

    @Modifying
    @Query(value = "insert into user_favorite_places (user_id, place_id, position) values (:userId, :placeId, :position)",
            nativeQuery = true)
    void addFavorite(UUID userId, UUID placeId, int position);

    interface FavoriteRow {

        UUID getUserId();

        UUID getId();

        String getType();

        String getNameEn();

        String getNameKm();

        Boolean getPopular();
    }
}
