package com.example.service.saas.service;

import com.example.service.saas.entity.SysUser;
import com.example.service.saas.entity.Tenant;
import com.example.service.saas.repository.SysUserRepository;
import com.example.service.saas.repository.TenantRepository;
import com.example.service.saas.util.WeComXmlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通讯录变更同步：接收企业微信 change_contact 事件，实时维护 sys_user。
 * 事件类型：create_user / update_user / delete_user（部门事件暂不处理）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactSyncService {

    private final SysUserRepository userRepository;
    private final TenantRepository tenantRepository;

    /** 异步处理，避免回调 5 秒超时 */
    @Async
    public void handleCallbackAsync(String plainXml) {
        try {
            handleCallback(plainXml);
        } catch (Exception e) {
            log.error("企微通讯录事件处理失败", e);
        }
    }

    @Transactional
    public void handleCallback(String plainXml) {
        Map<String, String> fields = WeComXmlUtil.parse(plainXml);
        String event = fields.get("Event");
        if (!"change_contact".equals(event)) {
            log.info("忽略非通讯录事件: event={}", event);
            return;
        }
        String changeType = fields.get("ChangeType");
        String wecomUserid = fields.get("UserName");
        switch (changeType) {
            case "create_user" -> createUser(fields, wecomUserid);
            case "update_user" -> updateUser(fields, wecomUserid);
            case "delete_user" -> disableUser(wecomUserid);
            default -> log.info("暂不处理的通讯录事件: changeType={}", changeType);
        }
    }

    private void createUser(Map<String, String> fields, String wecomUserid) {
        Optional<SysUser> exists = userRepository.findByWecomUserid(wecomUserid);
        if (exists.isPresent()) {
            updateUser(fields, wecomUserid);
            return;
        }
        Long tenantId = resolveTenantId(fields);
        if (tenantId == null) {
            log.warn("新增员工跳过：未找到对应租户, corpid={}", fields.get("AuthorizerCorpid"));
            return;
        }
        SysUser user = SysUser.builder()
                .tenantId(tenantId)
                .name(fields.get("Name"))
                .phone(fields.get("Mobile"))
                .role("staff")
                .wecomUserid(wecomUserid)
                .team(fields.get("Department"))
                .status(1)
                .build();
        userRepository.save(user);
        log.info("通讯录新增员工已同步: name={}", fields.get("Name"));
    }

    private void updateUser(Map<String, String> fields, String wecomUserid) {
        userRepository.findByWecomUserid(wecomUserid).ifPresent(user -> {
            if (fields.get("Name") != null) {
                user.setName(fields.get("Name"));
            }
            if (fields.get("Mobile") != null) {
                user.setPhone(fields.get("Mobile"));
            }
            user.setStatus(1);
            userRepository.save(user);
            log.info("通讯录员工信息已更新: name={}", fields.get("Name"));
        });
    }

    private void disableUser(String wecomUserid) {
        userRepository.findByWecomUserid(wecomUserid).ifPresent(user -> {
            user.setStatus(0);
            userRepository.save(user);
            log.info("通讯录员工已离职停用: name={}", user.getName());
        });
    }

    /**
     * 解析所属租户：第三方应用回调中 AuthorizerCorpid 为授权企业 corpid；
     * 自建应用场景 ToUserName 即 corpid。
     */
    private Long resolveTenantId(Map<String, String> fields) {
        String corpid = fields.get("AuthorizerCorpid");
        if (corpid == null || corpid.isBlank()) {
            corpid = fields.get("ToUserName");
        }
        if (corpid == null || corpid.isBlank()) {
            return null;
        }
        List<Tenant> tenants = tenantRepository.findByCorpid(corpid);
        return tenants.isEmpty() ? null : tenants.get(0).getId();
    }
}
