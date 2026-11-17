package com.example.zolieapi.repository;

import com.example.zolieapi.model.Comment;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    String VISIBLE = " from comments c join users a on a.id = c.author_id where c.deleted_at is null and "
            + VisibilitySql.AUTHOR_ACTIVE + " and " + VisibilitySql.NOT_BLOCKED;

    String NEWEST_FIRST = " and (c.created_at, c.id) < (:time, :id) order by c.created_at desc, c.id desc limit :limit ";

    String OLDEST_FIRST = " and (c.created_at, c.id) > (:time, :id) order by c.created_at, c.id limit :limit ";

    @Query(value = "select c.*" + VISIBLE + " and c.post_id = :postId and c.parent_id is null " + NEWEST_FIRST,
            nativeQuery = true)
    List<Comment> findForPost(UUID postId, UUID viewer, Instant time, UUID id, int limit);

    @Query(value = "select c.*" + VISIBLE + " and c.reel_id = :reelId and c.parent_id is null " + NEWEST_FIRST,
            nativeQuery = true)
    List<Comment> findForReel(UUID reelId, UUID viewer, Instant time, UUID id, int limit);

    @Query(value = "select c.*" + VISIBLE + " and c.parent_id = :parentId " + OLDEST_FIRST, nativeQuery = true)
    List<Comment> findReplies(UUID parentId, UUID viewer, Instant time, UUID id, int limit);

    @Query(value = """
            select parent_id as parentId, count(*) as total from comments
            where parent_id in (:parentIds) and deleted_at is null group by parent_id
            """, nativeQuery = true)
    List<ReplyCountRow> countReplies(Collection<UUID> parentIds);

    @Modifying
    @Query(value = """
            update comments set deleted_at = now()
            where deleted_at is null and (id = :commentId or parent_id = :commentId)
            """, nativeQuery = true)
    int softDeleteWithReplies(UUID commentId);

    interface ReplyCountRow {

        UUID getParentId();

        long getTotal();
    }
}
