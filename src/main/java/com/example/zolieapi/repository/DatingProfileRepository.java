package com.example.zolieapi.repository;

import com.example.zolieapi.model.DatingProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatingProfileRepository extends JpaRepository<DatingProfile, UUID> {
}
