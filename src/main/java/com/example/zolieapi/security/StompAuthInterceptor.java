package com.example.zolieapi.security;

import com.example.zolieapi.exception.ApiException;
import java.security.Principal;
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

    private final TokenAuthenticator tokenAuthenticator;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader(AUTHORIZATION)));
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
}
