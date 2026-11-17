package com.example.zolieapi.repository;

import com.example.zolieapi.model.PostMedia;
import com.example.zolieapi.model.id.PostMediaId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostMediaRepository extends JpaRepository<PostMedia, PostMediaId> {

    @Query("select m from PostMedia m where m.id.postId in :postIds order by m.id.postId, m.id.position")
    List<PostMedia> findByPostIds(Collection<UUID> postIds);
}
