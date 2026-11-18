package com.example.zolieapi.repository;

import com.example.zolieapi.model.Post;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface InsightRepository extends Repository<Post, UUID> {

    @Modifying
    @Query(value = """
            insert into profile_visits (profile_user_id, visitor_id) values (:profileUserId, :visitorId)
            on conflict do nothing
            """, nativeQuery = true)
    void recordProfileVisit(UUID profileUserId, UUID visitorId);

    @Modifying
    @Query(value = "insert into post_views (post_id, viewer_id) values (:postId, :viewerId) on conflict do nothing",
            nativeQuery = true)
    void recordPostView(UUID postId, UUID viewerId);

    @Query(value = """
            select count(distinct v.viewer_id) from post_views v join posts p on p.id = v.post_id
            where p.author_id = :userId and v.viewed_at >= now() - make_interval(days => :days)
            """, nativeQuery = true)
    long countAccountsReached(UUID userId, int days);

    @Query(value = """
            select count(*) from profile_visits
            where profile_user_id = :userId and visit_date > current_date - :days
            """, nativeQuery = true)
    long countProfileVisits(UUID userId, int days);

    @Query(value = """
            select count(*) from follows
            where followed_id = :userId and status = 'accepted' and accepted_at >= now() - make_interval(days => :days)
            """, nativeQuery = true)
    long countNewFollowers(UUID userId, int days);

    @Query(value = """
            select to_char(d.day, 'YYYY-MM-DD') as day,
                   (select count(distinct v.viewer_id) from post_views v join posts p on p.id = v.post_id
                     where p.author_id = :userId and v.viewed_at::date = d.day) as reach,
                   (select count(*) from profile_visits pv
                     where pv.profile_user_id = :userId and pv.visit_date = d.day) as profileVisits,
                   (select count(*) from follows f
                     where f.followed_id = :userId and f.status = 'accepted' and f.accepted_at::date = d.day) as newFollowers
            from (select generate_series(current_date - (:days - 1), current_date, interval '1 day')::date as day) d
            order by d.day
            """, nativeQuery = true)
    List<DailyRow> findDaily(UUID userId, int days);

    @Query(value = """
            select p.id as postId, p.type as type, p.text as caption, p.like_count as likeCount,
                   p.comment_count as commentCount, count(v.viewer_id) as reach
            from posts p left join post_views v on v.post_id = p.id
            where p.author_id = :userId and p.deleted_at is null
              and p.created_at >= now() - make_interval(days => :days)
            group by p.id
            order by count(v.viewer_id) desc, p.like_count desc, p.created_at desc limit 5
            """, nativeQuery = true)
    List<TopPostRow> findTopPosts(UUID userId, int days);

    @Query(value = """
            select coalesce(u.city, 'Other') as city, count(distinct v.viewer_id) as viewers
            from post_views v join posts p on p.id = v.post_id join users u on u.id = v.viewer_id
            where p.author_id = :userId and v.viewed_at >= now() - make_interval(days => :days)
            group by coalesce(u.city, 'Other') order by count(distinct v.viewer_id) desc limit 5
            """, nativeQuery = true)
    List<AudienceRow> findAudience(UUID userId, int days);

    @Query(value = """
            select v.post_id as postId, count(*) as reach from post_views v
            where v.post_id in (:postIds) group by v.post_id
            """, nativeQuery = true)
    List<ReachRow> countReach(Collection<UUID> postIds);

    interface DailyRow {

        String getDay();

        long getReach();

        long getProfileVisits();

        long getNewFollowers();
    }

    interface TopPostRow {

        UUID getPostId();

        String getType();

        String getCaption();

        int getLikeCount();

        int getCommentCount();

        long getReach();
    }

    interface AudienceRow {

        String getCity();

        long getViewers();
    }

    interface ReachRow {

        UUID getPostId();

        long getReach();
    }
}
