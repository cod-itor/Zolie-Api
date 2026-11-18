package com.example.zolieapi.security;

import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.model.enums.AccountStatus;
import com.example.zolieapi.repository.UserRepository;
import java.security.Principal;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String APP_PREFIX = "/app/";

    private static final Set<String> SUBSCRIBABLE = Set.of(
            "/user/queue/messages",
            "/user/queue/acks",
            "/user/queue/receipts",
            "/user/queue/reactions",
            "/user/queue/typing",
            "/user/queue/notifications",
            "/user/queue/errors");

    private final TokenAuthenticator tokenAuthenticator;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> accessor.setUser(authenticate(accessor.getFirstNativeHeader(AUTHORIZATION)));
            case SUBSCRIBE -> {
                requireUser(accessor);
                if (!SUBSCRIBABLE.contains(accessor.getDestination())) {
                    throw new MessagingException("FORBIDDEN");
                }
            }
            case SEND -> {
                Principal user = requireUser(accessor);
                String destination = accessor.getDestination();
                if (destination == null || !destination.startsWith(APP_PREFIX)) {
                    throw new MessagingException("FORBIDDEN");
                }
                requireActive(user);
            }
            default -> {
            }
        }
        return message;
    }

    private Principal authenticate(String header) {
        if (header == null || !header.startsWith(BEARER)) {
            throw new MessagingException("AUTH_REQUIRED");
        }
        try {
            AuthUser authUser = tokenAuthenticator.authenticate(header.substring(BEARER.length()).trim());
            if (!authUser.isSynced() || authUser.suspended()) {
                throw new MessagingException("FORBIDDEN");
            }
            String userId = authUser.userId().toString();
            return () -> userId;
        } catch (ApiException e) {
            throw new MessagingException(e.getCode().name());
        }
    }

    private Principal requireUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new MessagingException("AUTH_REQUIRED");
        }
        return accessor.getUser();
    }

    private void requireActive(Principal user) {
        boolean active = userRepository.findById(UUID.fromString(user.getName()))
                .filter(found -> found.getStatus() == AccountStatus.ACTIVE)
                .isPresent();
        if (!active) {
            throw new MessagingException("ACCOUNT_SUSPENDED");
        }
    }
}
