package com.prismnetai.repository;

import com.prismnetai.entity.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiRequestRepository extends JpaRepository<AiRequest, Long> {

    List<AiRequest> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime start, LocalDateTime end);

    List<AiRequest> findByStatus(AiRequest.RequestStatus status);

    @Query("SELECT r FROM AiRequest r WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    List<AiRequest> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

    @Query("SELECT r FROM AiRequest r WHERE r.selectedProvider.id = :providerId AND r.createdAt >= :since")
    List<AiRequest> findByProviderIdAndCreatedAtAfter(@Param("providerId") Long providerId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(r) FROM AiRequest r WHERE r.status = 'COMPLETED' AND r.createdAt >= :since")
    long countCompletedRequestsSince(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(r.latencyMs) FROM AiRequest r WHERE r.status = 'COMPLETED' AND r.createdAt >= :since")
    Double getAverageLatencySince(@Param("since") LocalDateTime since);
}