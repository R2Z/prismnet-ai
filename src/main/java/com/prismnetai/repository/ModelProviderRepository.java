package com.prismnetai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.prismnetai.entity.ModelProvider;

@Repository
public interface ModelProviderRepository extends JpaRepository<ModelProvider, Long> {
}