package com.example.zolieapi.repository;

import com.example.zolieapi.model.DatingPrompt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatingPromptRepository extends JpaRepository<DatingPrompt, Integer> {

    List<DatingPrompt> findByActiveTrueOrderBySortOrderAsc();
}
