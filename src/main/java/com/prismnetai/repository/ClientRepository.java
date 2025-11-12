package com.prismnetai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prismnetai.entity.Client;

public interface ClientRepository extends JpaRepository<Client, String> {
}
