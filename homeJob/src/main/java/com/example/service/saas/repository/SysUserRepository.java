package com.example.service.saas.repository;

import com.example.service.saas.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByIdAndTenantId(Long id, Long tenantId);

    /** 企业微信 userid 反查系统用户（OAuth 登录绑定） */
    Optional<SysUser> findByWecomUserid(String wecomUserid);

    /** 门店下的服务员工列表（供店长排班选择） */
    List<SysUser> findByTenantIdAndRoleAndStatus(Long tenantId, String role, Integer status);
}
