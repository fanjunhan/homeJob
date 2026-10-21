package com.example.service.saas.repository;

import com.example.service.saas.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    List<Tenant> findByCorpid(String corpid);

    Optional<Tenant> findByCorpidAndStatus(String corpid, Integer status);
}
