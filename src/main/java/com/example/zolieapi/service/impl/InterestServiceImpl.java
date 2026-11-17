package com.example.zolieapi.service.impl;

import com.example.zolieapi.dto.response.InterestResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.model.Interest;
import com.example.zolieapi.repository.InterestRepository;
import com.example.zolieapi.service.InterestService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterestServiceImpl implements InterestService {

    private static final int MINIMUM_INTERESTS = 3;

    private final InterestRepository interestRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InterestResponse> listAll() {
        return toResponses(interestRepository.findAllByOrderByIdAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterestResponse> listMine(UUID userId) {
        return toResponses(interestRepository.findByUser(userId));
    }

    @Override
    @Transactional
    public List<InterestResponse> replaceMine(UUID userId, List<Integer> interestIds) {
        List<Integer> ids = interestIds.stream().distinct().toList();
        if (ids.size() < MINIMUM_INTERESTS) {
            throw ApiException.rule("Pick at least " + MINIMUM_INTERESTS + " interests.");
        }
        if (interestRepository.findAllById(ids).size() != ids.size()) {
            throw ApiException.rule("One or more interests do not exist.");
        }
        interestRepository.deleteForUser(userId);
        ids.forEach(id -> interestRepository.addForUser(userId, id));
        return toResponses(interestRepository.findByUser(userId));
    }

    private List<InterestResponse> toResponses(List<Interest> interests) {
        return interests.stream()
                .map(interest -> new InterestResponse(interest.getId(), interest.getNameEn(), interest.getNameKm()))
                .toList();
    }
}
