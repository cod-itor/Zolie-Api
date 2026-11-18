package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.PostType;
import com.example.zolieapi.model.enums.Visibility;
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
@Table(name = "posts")
@DynamicUpdate
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID authorId;

    private PostType type;

    private String text;

    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    private Instant pollEndsAt;

    private int likeCount;

    private int commentCount;

    private int shareCount;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant deletedAt;

    private UUID placeId;

    private UUID soundId;

    private String textBackground;

    private String textStyle;

    @Builder.Default
    private boolean commentsEnabled = true;

    private boolean hideLikeCount;

    private Instant pinnedAt;

    private boolean pollAllowMultiple;

    @Builder.Default
    private boolean pollShowResultsFirst = true;
}
