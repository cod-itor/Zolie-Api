package com.example.zolieapi.repository;

import com.example.zolieapi.model.DatingIntro;
import com.example.zolieapi.model.id.UserPairId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatingIntroRepository extends JpaRepository<DatingIntro, UserPairId> {
}
