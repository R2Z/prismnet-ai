package com.prismnetai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.prismnetai.entity.Provider;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByName(String name);

    List<Provider> findByNameAndIsActiveTrue(String name);

    List<Provider> findByIsActiveTrue();

    @Query("SELECT p FROM Provider p WHERE p.isActive = true ORDER BY p.name")
    List<Provider> findActiveProvidersOrderedByName();

    @Query("SELECT p FROM Provider p WHERE p.isActive = true AND p.id IN :ids")
    List<Provider> findActiveProvidersByIds(@Param("ids") List<Long> ids);
}