package com.example.zolieapi.service.impl;

import com.example.zolieapi.model.DeviceToken;
import com.example.zolieapi.repository.DeviceTokenRepository;
import com.example.zolieapi.service.PushService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushServiceImpl implements PushService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final ObjectProvider<FirebaseMessaging> firebaseMessaging;

    @Override
    @Transactional
    public void send(UUID userId, Map<String, String> data) {
        FirebaseMessaging messaging = firebaseMessaging.getIfAvailable();
        if (messaging == null) {
            log.debug("Push skipped (Firebase not configured): user={} type={}", userId, data.get("type"));
            return;
        }
        for (DeviceToken device : deviceTokenRepository.findByUserId(userId)) {
            sendToDevice(messaging, device, data);
        }
    }

    private void sendToDevice(FirebaseMessaging messaging, DeviceToken device, Map<String, String> data) {
        try {
            messaging.send(Message.builder().setToken(device.getToken()).putAllData(data).build());
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                deviceTokenRepository.delete(device);
            } else {
                log.warn("Push failed: user={} code={}", device.getUserId(), e.getMessagingErrorCode());
            }
        }
    }
}
