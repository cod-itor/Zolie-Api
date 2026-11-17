package com.example.zolieapi.model.id;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKeyId implements Serializable {

    private UUID userId;
    private UUID key;
}
