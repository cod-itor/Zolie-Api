package com.example.zolieapi.repository;

import com.example.zolieapi.model.DatingLike;
import com.example.zolieapi.model.id.UserPairId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DatingLikeRepository extends JpaRepository<DatingLike, UserPairId> {

    String RECEIVED = """
            select l.* from dating_likes l
            join dating_profiles c on c.user_id = l.from_user_id
            join users a on a.id = l.from_user_id
            where l.to_user_id = :viewer and c.status = 'active' and
            """ + VisibilitySql.AUTHOR_ACTIVE + " and " + VisibilitySql.NOT_BLOCKED + """
              and not exists (select 1 from dating_likes mine
                  where mine.from_user_id = :viewer and mine.to_user_id = l.from_user_id)
              and not exists (select 1 from dating_passes p
                  where p.from_user_id = :viewer and p.to_user_id = l.from_user_id)
            """;

    String PAGE = " and (l.created_at, l.from_user_id) < (:time, :id) order by l.created_at desc, l.from_user_id desc limit :limit ";

    @Query(value = RECEIVED + PAGE, nativeQuery = true)
    List<DatingLike> findReceived(UUID viewer, Instant time, UUID id, int limit);

    @Query(value = RECEIVED + " and l.type = 'intro' " + PAGE, nativeQuery = true)
    List<DatingLike> findReceivedIntros(UUID viewer, Instant time, UUID id, int limit);

    @Modifying
    @Query(value = "insert into dating_passes (from_user_id, to_user_id) values (:fromUserId, :toUserId) on conflict do nothing",
            nativeQuery = true)
    void insertPass(UUID fromUserId, UUID toUserId);

    @Query(value = "select exists (select 1 from dating_passes where from_user_id = :fromUserId and to_user_id = :toUserId)",
            nativeQuery = true)
    boolean existsPass(UUID fromUserId, UUID toUserId);
}
