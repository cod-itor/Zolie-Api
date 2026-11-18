package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.response.HelpArticleResponse;
import com.example.zolieapi.model.HelpArticle;
import com.example.zolieapi.model.enums.HelpTopic;
import com.example.zolieapi.repository.HelpArticleRepository;
import com.example.zolieapi.service.HelpService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HelpServiceImpl implements HelpService {

    private final HelpArticleRepository helpArticleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HelpArticleResponse> list(String topic) {
        List<HelpArticle> articles = topic == null || topic.isBlank()
                ? helpArticleRepository.findAllByOrderBySortOrderAsc()
                : helpArticleRepository.findByTopicOrderBySortOrderAsc(Enums.parse(HelpTopic.class, topic, "topic"));
        return articles.stream()
                .map(article -> new HelpArticleResponse(article.getId(), Enums.value(article.getTopic()),
                        article.getTitleEn(), article.getTitleKm(), article.getBodyEn(), article.getBodyKm(),
                        article.isPopular()))
                .toList();
    }
}
