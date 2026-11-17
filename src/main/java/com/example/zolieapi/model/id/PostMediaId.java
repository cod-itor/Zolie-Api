package com.example.zolieapi.model.id;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostMediaId implements Serializable {

    private UUID postId;
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer position;
}
