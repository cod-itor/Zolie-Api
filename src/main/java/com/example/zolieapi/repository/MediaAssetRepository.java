package com.example.zolieapi.repository;

import com.example.zolieapi.model.MediaAsset;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
}
