package com.example.service.saas.repository;

import com.example.service.saas.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByTenantIdAndStatus(Long tenantId, Integer status);
}
