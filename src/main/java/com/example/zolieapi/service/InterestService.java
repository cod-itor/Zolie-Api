package com.example.zolieapi.service;

import com.example.zolieapi.dto.response.InterestResponse;
import java.util.List;
import java.util.UUID;

public interface InterestService {

    List<InterestResponse> listAll();

    List<InterestResponse> listMine(UUID userId);

    List<InterestResponse> replaceMine(UUID userId, List<Integer> interestIds);
}
