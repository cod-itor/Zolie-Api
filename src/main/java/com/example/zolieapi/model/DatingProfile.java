package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.DatingStatus;
import com.example.zolieapi.model.enums.Gender;
import com.example.zolieapi.model.enums.LookingFor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "dating_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatingProfile {

    @Id
    private UUID userId;

    @Builder.Default
    private DatingStatus status = DatingStatus.ACTIVE;

    private String bio;

    private Gender gender;

    @Builder.Default
    private LookingFor lookingFor = LookingFor.NOT_SURE;

    private String education;

    private String occupation;

    private String city;

    private Instant locationUpdatedAt;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "varchar(12)[]")
    @Builder.Default
    private String[] prefGenders = new String[]{"woman", "man", "nonbinary"};

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Builder.Default
    private Integer prefMinAge = 18;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Builder.Default
    private Integer prefMaxAge = 99;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Builder.Default
    private Integer maxDistanceKm = 25;

    @Builder.Default
    private boolean showAge = true;

    @Builder.Default
    private boolean showCity = true;

    private boolean showEducation;

    private boolean showOccupation;

    @Builder.Default
    private boolean showDistance = true;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
