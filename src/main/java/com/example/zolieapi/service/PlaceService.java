package com.example.zolieapi.service;

import com.example.zolieapi.dto.response.PlaceResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PlaceService {

    List<PlaceResponse> search(String query);

    List<PlaceResponse> listPopular();

    PlaceResponse get(UUID placeId);

    Map<UUID, PlaceResponse> byIds(Collection<UUID> placeIds);

    void requireExists(UUID placeId);

    List<PlaceResponse> favoritesOf(UUID userId);

    void replaceFavorites(UUID userId, List<UUID> placeIds);
}
