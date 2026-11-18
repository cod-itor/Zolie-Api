package com.example.zolieapi.repository;

import com.example.zolieapi.model.Report;
import com.example.zolieapi.model.enums.ReportTarget;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(UUID reporterId, ReportTarget targetType, UUID targetId);

    @Query(value = """
            select r.* from reports r
            where r.reporter_id = :reporterId and (r.created_at, r.id) < (:time, :id)
            order by r.created_at desc, r.id desc limit :limit
            """, nativeQuery = true)
    List<Report> findByReporter(UUID reporterId, Instant time, UUID id, int limit);

    @Query(value = """
            select r.* from reports r
            where r.status = :status and (r.created_at, r.id) > (:time, :id)
            order by r.created_at, r.id limit :limit
            """, nativeQuery = true)
    List<Report> findQueue(String status, Instant time, UUID id, int limit);

    List<Report> findTop50ByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTarget targetType, UUID targetId);
}
