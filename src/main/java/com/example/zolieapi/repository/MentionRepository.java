package com.example.zolieapi.repository;

import com.example.zolieapi.model.Mention;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentionRepository extends JpaRepository<Mention, UUID> {

    List<Mention> findByPostId(UUID postId);
}
