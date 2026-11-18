package com.example.zolieapi.repository;

import com.example.zolieapi.model.Hashtag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface HashtagRepository extends JpaRepository<Hashtag, UUID> {

    List<Hashtag> findByTagIn(Collection<String> tags);

    @Modifying
    @Query(value = "insert into hashtags (tag) values (:tag) on conflict do nothing", nativeQuery = true)
    void insertIfAbsent(String tag);

    @Modifying
    @Query(value = "delete from post_hashtags where post_id = :postId", nativeQuery = true)
    void clearPost(UUID postId);

    @Modifying
    @Query(value = "insert into post_hashtags (post_id, hashtag_id) values (:postId, :hashtagId) on conflict do nothing",
            nativeQuery = true)
    void tagPost(UUID postId, UUID hashtagId);

    @Modifying
    @Query(value = "insert into reel_hashtags (reel_id, hashtag_id) values (:reelId, :hashtagId) on conflict do nothing",
            nativeQuery = true)
    void tagReel(UUID reelId, UUID hashtagId);

    @Query(value = """
            select h.tag as tag, count(*) as total
            from hashtags h join post_hashtags ph on ph.hashtag_id = h.id join posts p on p.id = ph.post_id
            join users a on a.id = p.author_id
            where ph.created_at >= now() - interval '7 days' and p.deleted_at is null and p.visibility = 'public'
              and a.account_type = 'public' and a.status = 'active'
            group by h.tag order by count(*) desc, h.tag limit :limit
            """, nativeQuery = true)
    List<TagCountRow> findTrending(int limit);

    @Query(value = """
            select h.tag as tag, count(*) as total
            from hashtags h join post_hashtags ph on ph.hashtag_id = h.id join posts p on p.id = ph.post_id
            join users a on a.id = p.author_id
            where h.tag ilike :pattern escape '\\' and p.deleted_at is null and p.visibility = 'public'
              and a.account_type = 'public' and a.status = 'active'
            group by h.tag order by count(*) desc, h.tag limit :limit
            """, nativeQuery = true)
    List<TagCountRow> search(String pattern, int limit);

    interface TagCountRow {

        String getTag();

        long getTotal();
    }
}
