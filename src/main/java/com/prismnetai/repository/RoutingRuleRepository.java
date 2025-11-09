package com.prismnetai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.prismnetai.entity.RoutingRule;

@Repository
public interface RoutingRuleRepository extends JpaRepository<RoutingRule, Long> {

    Optional<RoutingRule> findByUserIdAndName(String userId, String name);

    List<RoutingRule> findByUserIdAndIsActiveTrue(String userId);

    @Query("SELECT rr FROM RoutingRule rr WHERE rr.userId = :userId AND rr.isActive = true ORDER BY rr.id ASC")
    List<RoutingRule> findActiveRulesByUserIdOrderedById(@Param("userId") String userId);
}