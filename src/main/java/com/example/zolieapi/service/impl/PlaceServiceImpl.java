package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.response.PlaceResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.model.Place;
import com.example.zolieapi.repository.PlaceRepository;
import com.example.zolieapi.service.PlaceService;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService {

    private static final int MAX_QUERY = 50;
    private static final int MAX_RESULTS = 20;
    private static final int MAX_FAVORITES = 6;

    private final PlaceRepository placeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PlaceResponse> search(String query) {
        String text = Texts.clean(query, MAX_QUERY, "q");
        if (text == null) {
            return listPopular();
        }
        return placeRepository.search(Texts.likePattern(text), MAX_RESULTS).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlaceResponse> listPopular() {
        return placeRepository.findTop20ByPopularTrueOrderByNameEnAsc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceResponse get(UUID placeId) {
        return placeRepository.findById(placeId).map(this::toResponse).orElseThrow(ApiException::notFound);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, PlaceResponse> byIds(Collection<UUID> placeIds) {
        List<UUID> ids = placeIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return placeRepository.findAllById(ids).stream()
                .map(this::toResponse)
                .collect(Collectors.toMap(PlaceResponse::id, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public void requireExists(UUID placeId) {
        if (placeId != null && !placeRepository.existsById(placeId)) {
            throw ApiException.rule("The place does not exist.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlaceResponse> favoritesOf(UUID userId) {
        return placeRepository.findFavorites(List.of(userId)).stream()
                .map(row -> new PlaceResponse(row.getId(), row.getType(), row.getNameEn(), row.getNameKm(),
                        Boolean.TRUE.equals(row.getPopular())))
                .toList();
    }

    @Override
    @Transactional
    public void replaceFavorites(UUID userId, List<UUID> placeIds) {
        List<UUID> ids = placeIds.stream().distinct().toList();
        if (ids.size() > MAX_FAVORITES) {
            throw ApiException.rule("You can pick up to " + MAX_FAVORITES + " places.");
        }
        if (placeRepository.findAllById(ids).size() != ids.size()) {
            throw ApiException.rule("One or more places do not exist.");
        }
        placeRepository.deleteFavorites(userId);
        for (int position = 0; position < ids.size(); position++) {
            placeRepository.addFavorite(userId, ids.get(position), position);
        }
    }

    private PlaceResponse toResponse(Place place) {
        return new PlaceResponse(place.getId(), Enums.value(place.getType()), place.getNameEn(), place.getNameKm(),
                place.isPopular());
    }
}
