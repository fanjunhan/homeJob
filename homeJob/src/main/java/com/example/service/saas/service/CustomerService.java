package com.example.service.saas.service;

import com.example.service.saas.common.BizException;
import com.example.service.saas.entity.Customer;
import com.example.service.saas.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** 客户档案维护（店长端） */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Page<Customer> page(Long tenantId, int pageNum, int pageSize) {
        return customerRepository.findByTenantId(tenantId, PageRequest.of(pageNum - 1, pageSize));
    }

    public Customer get(Long tenantId, Long id) {
        return customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BizException("客户不存在"));
    }

    /** 小程序登录：按手机号匹配档案 */
    public Optional<Customer> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        return customerRepository.findByPhoneAndStatus(phone, 1);
    }

    /** 小程序登录：绑定 openid（首次登录或换微信号时更新） */
    @Transactional
    public Customer bindOpenid(Customer customer, String openid) {
        if (!openid.equals(customer.getOpenid())) {
            customer.setOpenid(openid);
            return customerRepository.save(customer);
        }
        return customer;
    }

    @Transactional
    public Customer create(Long tenantId, Customer form, Long operatorId) {
        Customer customer = new Customer();
        copy(form, customer);
        customer.setTenantId(tenantId);
        customer.setStatus(1);
        customer.setCreatedBy(operatorId);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long tenantId, Long id, Customer form, Long operatorId) {
        Customer customer = get(tenantId, id);
        copy(form, customer);
        return customerRepository.save(customer);
    }

    @Transactional
    public void disable(Long tenantId, Long id) {
        Customer customer = get(tenantId, id);
        customer.setStatus(0);
        customerRepository.save(customer);
    }

    private void copy(Customer form, Customer customer) {
        customer.setName(form.getName());
        customer.setPhone(form.getPhone());
        customer.setGender(form.getGender());
        customer.setAddress(form.getAddress());
        customer.setServiceItemId(form.getServiceItemId());
        customer.setCycleDesc(form.getCycleDesc());
        customer.setContractStart(form.getContractStart());
        customer.setContractEnd(form.getContractEnd());
        customer.setRemark(form.getRemark());
        customer.setOwnerStaffId(form.getOwnerStaffId());
    }
}
