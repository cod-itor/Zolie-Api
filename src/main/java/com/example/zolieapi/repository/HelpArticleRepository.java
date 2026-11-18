package com.example.zolieapi.repository;

import com.example.zolieapi.model.HelpArticle;
import com.example.zolieapi.model.enums.HelpTopic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HelpArticleRepository extends JpaRepository<HelpArticle, Integer> {

    List<HelpArticle> findAllByOrderBySortOrderAsc();

    List<HelpArticle> findByTopicOrderBySortOrderAsc(HelpTopic topic);
}
