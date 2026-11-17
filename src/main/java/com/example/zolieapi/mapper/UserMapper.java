package com.example.zolieapi.mapper;

import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.model.User;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.MediaService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final UserRepository userRepository;
    private final MediaService mediaService;

    public UserSummaryResponse toSummary(User user) {
        return toSummaries(List.of(user)).get(0);
    }

    public List<UserSummaryResponse> toSummaries(List<User> users) {
        Map<UUID, String> avatars = mediaService.urls(users.stream().map(User::getProfilePhotoId).toList());
        return users.stream()
                .map(user -> new UserSummaryResponse(user.getId(), user.getUsername(), user.getDisplayName(),
                        avatars.get(user.getProfilePhotoId())))
                .toList();
    }

    public Map<UUID, UserSummaryResponse> summariesById(Collection<UUID> userIds) {
        List<User> users = userRepository.findAllById(userIds.stream().distinct().toList());
        return toSummaries(users).stream().collect(Collectors.toMap(UserSummaryResponse::id, Function.identity()));
    }
}
