package com.prismnetai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;

@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {

    Optional<Model> findByModelIdAndProvider(String modelId, Provider provider);

    List<Model> findByProviderAndIsActiveTrue(Provider provider);

    List<Model> findByIsActiveTrue();

    @Query("SELECT m FROM Model m WHERE m.isActive = true ORDER BY m.provider.name, m.name")
    List<Model> findActiveModelsOrderedByProviderAndName();

    @Query("SELECT m FROM Model m WHERE m.isActive = true AND m.provider.id IN :providerIds")
    List<Model> findActiveModelsByProviderIds(@Param("providerIds") List<Long> providerIds);

    @Query("SELECT m FROM Model m WHERE m.isActive = true ORDER BY (m.inputPricing + m.outputPricing) ASC")
    List<Model> findActiveModelsOrderedByLowestCost();

    @Query("SELECT m FROM Model m WHERE m.isActive = true AND m.modelId = :modelId AND m.provider.id IN :providerIds")
    List<Model> findActiveModelsByModelIdAndProviderIds(@Param("modelId") String modelId, @Param("providerIds") List<Long> providerIds);

    @Query("SELECT m FROM Model m WHERE m.isActive = true AND m.modelId = :modelId AND m.provider = :provider")
    Optional<Model> findActiveByModelIdAndProvider(@Param("modelId") String modelId, @Param("provider") Provider provider);
}