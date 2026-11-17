package com.example.zolieapi.repository;

import com.example.zolieapi.model.CloseFriend;
import com.example.zolieapi.model.id.CloseFriendId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CloseFriendRepository extends JpaRepository<CloseFriend, CloseFriendId> {

    @Modifying
    @Query(value = """
            delete from close_friends
            where (owner_id = :first and friend_id = :second) or (owner_id = :second and friend_id = :first)
            """, nativeQuery = true)
    void deleteBetween(UUID first, UUID second);
}
