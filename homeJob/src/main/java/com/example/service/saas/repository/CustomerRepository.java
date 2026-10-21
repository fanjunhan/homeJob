package com.example.service.saas.repository;

import com.example.service.saas.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdAndTenantId(Long id, Long tenantId);

    /** 小程序登录：按手机号匹配客户档案 */
    Optional<Customer> findByPhoneAndStatus(String phone, Integer status);

    Page<Customer> findByTenantId(Long tenantId, Pageable pageable);
}
