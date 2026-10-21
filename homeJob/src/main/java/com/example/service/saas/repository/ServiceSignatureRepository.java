package com.example.service.saas.repository;

import com.example.service.saas.entity.ServiceSignature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceSignatureRepository extends JpaRepository<ServiceSignature, Long> {
}
