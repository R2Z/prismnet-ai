package com.prismnetai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prismnetai.entity.ApiKey;

public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
}
