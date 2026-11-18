package com.example.zolieapi.repository;

import com.example.zolieapi.model.UserNote;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserNoteRepository extends JpaRepository<UserNote, UUID> {

    @Query(value = """
            select n.* from user_notes n join users a on a.id = n.user_id
            where n.expires_at > now() and
            """ + VisibilitySql.AUTHOR_ACTIVE + " and " + VisibilitySql.NOT_BLOCKED
            + " and (a.id = :viewer or " + VisibilitySql.FOLLOWS_AUTHOR + ")" + """
            order by (a.id = :viewer) desc, n.created_at desc limit 50
            """, nativeQuery = true)
    List<UserNote> findFeed(UUID viewer);

    @Modifying
    @Query("delete from UserNote n where n.expiresAt <= :now")
    int deleteExpired(Instant now);
}
