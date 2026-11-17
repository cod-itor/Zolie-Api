package com.example.zolieapi.model;

import com.example.zolieapi.model.id.PostMediaId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "post_media")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostMedia {

    @EmbeddedId
    private PostMediaId id;

    private UUID mediaId;
}
