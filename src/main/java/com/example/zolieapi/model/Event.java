package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.EventCategory;
import com.example.zolieapi.model.enums.EventVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "events")
@DynamicUpdate
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID hostId;

    private String title;

    private String description;

    @Builder.Default
    private EventCategory category = EventCategory.OTHER;

    private UUID coverMediaId;

    private Instant startsAt;

    private Instant endsAt;

    private UUID placeId;

    private String venueName;

    @Column(name = "is_free")
    @Builder.Default
    private boolean free = true;

    private String priceNote;

    @Builder.Default
    private EventVisibility visibility = EventVisibility.PUBLIC;

    @Builder.Default
    private boolean showGuestList = true;

    private int interestedCount;

    private int goingCount;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant cancelledAt;
}
