package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.AccountStatus;
import com.example.zolieapi.model.enums.AccountType;
import com.example.zolieapi.model.enums.AudienceRule;
import com.example.zolieapi.model.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "users")
@DynamicUpdate
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String firebaseUid;

    private String email;

    private String username;

    private String displayName;

    private String bio;

    private UUID profilePhotoId;

    @Column(updatable = false)
    private LocalDate dateOfBirth;

    @Builder.Default
    private AccountType accountType = AccountType.PUBLIC;

    @Builder.Default
    private UserRole role = UserRole.USER;

    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Builder.Default
    private String locale = "km";

    @Builder.Default
    private String theme = "system";

    @Builder.Default
    private boolean showOnlineStatus = true;

    private Instant lastActiveAt;

    private Instant suspendedUntil;

    private Instant termsAcceptedAt;

    private int followerCount;

    private int followingCount;

    private int postCount;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant deletedAt;

    private String khmerName;

    private String websiteUrl;

    private String city;

    private UUID coverPhotoId;

    private boolean dataSaver;

    @Builder.Default
    private boolean readReceipts = true;

    @Builder.Default
    private AudienceRule whoCanMessage = AudienceRule.EVERYONE;

    @Builder.Default
    private AudienceRule whoCanMention = AudienceRule.EVERYONE;

    @Builder.Default
    private AudienceRule storyRepliesFrom = AudienceRule.EVERYONE;

    private boolean hideDatingFromFollowing;

    private Instant deletionRequestedAt;

    private Instant deleteAfter;

    private String deletionReason;
}
