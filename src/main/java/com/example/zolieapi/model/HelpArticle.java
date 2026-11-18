package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.HelpTopic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "help_articles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelpArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer id;

    private HelpTopic topic;

    private String titleEn;

    private String titleKm;

    private String bodyEn;

    private String bodyKm;

    @Column(name = "is_popular")
    private boolean popular;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer sortOrder;
}
