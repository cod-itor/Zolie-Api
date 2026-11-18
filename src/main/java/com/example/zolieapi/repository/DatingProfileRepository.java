package com.example.zolieapi.repository;

import com.example.zolieapi.model.DatingProfile;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DatingProfileRepository extends JpaRepository<DatingProfile, UUID> {

    String COMPATIBLE = " c.status = 'active' and " + VisibilitySql.AUTHOR_ACTIVE + " and " + VisibilitySql.NOT_BLOCKED + """
              and age_years(a.date_of_birth) >= 18
              and age_years(a.date_of_birth) between me.pref_min_age and me.pref_max_age
              and age_years(mu.date_of_birth) between c.pref_min_age and c.pref_max_age
              and c.gender = any(me.pref_genders)
              and me.gender = any(c.pref_genders)
              and ST_DWithin(me.location, c.location, least(me.max_distance_km, c.max_distance_km) * 1000)
              and not ((mu.hide_dating_from_following or a.hide_dating_from_following) and exists (
                  select 1 from follows hf where hf.status = 'accepted'
                    and ((hf.follower_id = mu.id and hf.followed_id = a.id)
                      or (hf.follower_id = a.id and hf.followed_id = mu.id))))
            """;

    @Query(value = """
            select exists (select 1
                from dating_profiles me
                join users mu on mu.id = me.user_id
                join dating_profiles c on c.user_id = :userId
                join users a on a.id = c.user_id
                where me.user_id = :viewer and
            """ + COMPATIBLE + ")", nativeQuery = true)
    boolean isCompatible(UUID userId, UUID viewer);

    @Modifying
    @Query(value = """
            update dating_profiles
            set location = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, location_updated_at = now()
            where user_id = :userId
            """, nativeQuery = true)
    void setLocation(UUID userId, double lat, double lng);

    @Query(value = "select coalesce((select location is not null from dating_profiles where user_id = :userId), false)",
            nativeQuery = true)
    boolean hasLocation(UUID userId);

    @Query(value = """
            select c.user_id as userId, ST_Distance(me.location, c.location) as distanceMeters
            from dating_profiles me
            join users mu on mu.id = me.user_id
            join dating_profiles c on c.user_id <> me.user_id
            join users a on a.id = c.user_id
            where me.user_id = :viewer and
            """ + COMPATIBLE + """
              and not exists (select 1 from dating_likes l
                  where l.from_user_id = :viewer and l.to_user_id = c.user_id)
              and not exists (select 1 from dating_passes p
                  where p.from_user_id = :viewer and p.to_user_id = c.user_id)
            order by ST_Distance(me.location, c.location), c.user_id
            limit :limit
            """, nativeQuery = true)
    List<CandidateRow> findCandidates(UUID viewer, int limit);

    @Query(value = """
            select exists (select 1 from dating_profiles c join users a on a.id = c.user_id
                where c.user_id = :userId and c.status = 'active' and age_years(a.date_of_birth) >= 18 and
            """ + VisibilitySql.AUTHOR_ACTIVE + " and " + VisibilitySql.NOT_BLOCKED + ")", nativeQuery = true)
    boolean isDatable(UUID userId, UUID viewer);

    @Query(value = "select 1 from (select pg_advisory_xact_lock(hashtextextended(:pairKey, 0))) locked",
            nativeQuery = true)
    Integer lockPair(String pairKey);

    @Query(value = """
            select user_id as userId, media_id as mediaId from dating_profile_photos
            where user_id in (:userIds) order by user_id, position
            """, nativeQuery = true)
    List<PhotoRow> findPhotos(Collection<UUID> userIds);

    @Modifying
    @Query(value = "delete from dating_profile_photos where user_id = :userId", nativeQuery = true)
    void deletePhotos(UUID userId);

    @Modifying
    @Query(value = "insert into dating_profile_photos (user_id, media_id, position) values (:userId, :mediaId, :position)",
            nativeQuery = true)
    void addPhoto(UUID userId, UUID mediaId, int position);

    @Query(value = """
            select di.user_id as userId, i.id::int as id, i.name_en as nameEn, i.name_km as nameKm
            from dating_profile_interests di join interests i on i.id = di.interest_id
            where di.user_id in (:userIds) order by di.user_id, i.id
            """, nativeQuery = true)
    List<InterestRow> findInterests(Collection<UUID> userIds);

    @Query(value = """
            with mine as (
                select interest_id from dating_profile_interests where user_id = :viewer
                union select interest_id from user_interests where user_id = :viewer),
            theirs as (
                select user_id, interest_id from dating_profile_interests where user_id in (:userIds)
                union select user_id, interest_id from user_interests where user_id in (:userIds))
            select t.user_id as userId, i.id::int as id, i.name_en as nameEn, i.name_km as nameKm
            from theirs t join mine m on m.interest_id = t.interest_id join interests i on i.id = t.interest_id
            order by t.user_id, i.id
            """, nativeQuery = true)
    List<InterestRow> findSharedInterests(UUID viewer, Collection<UUID> userIds);

    @Query(value = """
            select (select count(*) from dating_likes l
                     where l.from_user_id = :viewer and l.created_at >= :since
                       and not exists (select 1 from dating_likes r
                           where r.from_user_id = l.to_user_id and r.to_user_id = :viewer and r.created_at < l.created_at))
                 + (select count(*) from dating_passes p
                     where p.from_user_id = :viewer and p.created_at >= :since
                       and not exists (select 1 from dating_likes r
                           where r.from_user_id = p.to_user_id and r.to_user_id = :viewer))
            """, nativeQuery = true)
    long countPicksUsed(UUID viewer, Instant since);

    interface CandidateRow {

        UUID getUserId();

        Double getDistanceMeters();
    }

    interface PhotoRow {

        UUID getUserId();

        UUID getMediaId();
    }

    interface InterestRow {

        UUID getUserId();

        Integer getId();

        String getNameEn();

        String getNameKm();
    }
}
