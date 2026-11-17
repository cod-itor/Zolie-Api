package com.example.zolieapi.repository;

import com.example.zolieapi.model.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
