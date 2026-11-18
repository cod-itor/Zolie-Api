package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.RsvpStatus;
import com.example.zolieapi.model.id.EventRsvpId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "event_rsvps")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRsvp {

    @EmbeddedId
    private EventRsvpId id;

    private RsvpStatus status;

    private boolean dayReminded;

    private boolean hourReminded;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
