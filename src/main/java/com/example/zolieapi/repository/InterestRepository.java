package com.example.zolieapi.repository;

import com.example.zolieapi.model.Interest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface InterestRepository extends JpaRepository<Interest, Integer> {

    List<Interest> findAllByOrderByIdAsc();

    @Query(value = """
            select i.* from interests i join user_interests ui on ui.interest_id = i.id
            where ui.user_id = :userId order by i.id
            """, nativeQuery = true)
    List<Interest> findByUser(UUID userId);

    @Modifying
    @Query(value = "delete from user_interests where user_id = :userId", nativeQuery = true)
    void deleteForUser(UUID userId);

    @Modifying
    @Query(value = "insert into user_interests (user_id, interest_id) values (:userId, :interestId)", nativeQuery = true)
    void addForUser(UUID userId, int interestId);

    @Query(value = """
            select i.* from interests i join dating_profile_interests di on di.interest_id = i.id
            where di.user_id = :userId order by i.id
            """, nativeQuery = true)
    List<Interest> findByDatingProfile(UUID userId);

    @Modifying
    @Query(value = "delete from dating_profile_interests where user_id = :userId", nativeQuery = true)
    void deleteForDatingProfile(UUID userId);

    @Modifying
    @Query(value = "insert into dating_profile_interests (user_id, interest_id) values (:userId, :interestId)",
            nativeQuery = true)
    void addForDatingProfile(UUID userId, int interestId);
}
