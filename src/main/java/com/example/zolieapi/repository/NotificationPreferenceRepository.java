package com.example.zolieapi.repository;

import com.example.zolieapi.model.NotificationPreference;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
}
