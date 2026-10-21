package com.example.service.saas.controller;

import com.example.service.saas.common.ApiResponse;
import com.example.service.saas.common.TenantContext;
import com.example.service.saas.entity.Customer;
import com.example.service.saas.service.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** 客户档案维护（店长端） */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /** 客户分页列表 */
    @GetMapping
    public ApiResponse<Page<Customer>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(customerService.page(TenantContext.tenantId(), page, size));
    }

    /** 客户详情 */
    @GetMapping("/{id}")
    public ApiResponse<Customer> detail(@PathVariable Long id) {
        return ApiResponse.ok(customerService.get(TenantContext.tenantId(), id));
    }

    /** 新增客户 */
    @PostMapping
    public ApiResponse<Customer> create(@Valid @RequestBody CustomerForm form) {
        Customer customer = new Customer();
        customer.setName(form.name());
        customer.setPhone(form.phone());
        customer.setGender(form.gender());
        customer.setAddress(form.address());
        customer.setServiceItemId(form.serviceItemId());
        customer.setCycleDesc(form.cycleDesc());
        customer.setContractStart(form.contractStart());
        customer.setContractEnd(form.contractEnd());
        customer.setRemark(form.remark());
        customer.setOwnerStaffId(form.ownerStaffId());
        return ApiResponse.ok(customerService.create(TenantContext.tenantId(), customer, TenantContext.userId()));
    }

    /** 修改客户 */
    @PutMapping("/{id}")
    public ApiResponse<Customer> update(@PathVariable Long id, @Valid @RequestBody CustomerForm form) {
        Customer entity = toEntity(form);
        return ApiResponse.ok(customerService.update(TenantContext.tenantId(), id, entity, TenantContext.userId()));
    }

    /** 停用客户 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        customerService.disable(TenantContext.tenantId(), id);
        return ApiResponse.ok();
    }

    private Customer toEntity(CustomerForm form) {
        Customer customer = new Customer();
        customer.setName(form.name());
        customer.setPhone(form.phone());
        customer.setGender(form.gender());
        customer.setAddress(form.address());
        customer.setServiceItemId(form.serviceItemId());
        customer.setCycleDesc(form.cycleDesc());
        customer.setContractStart(form.contractStart());
        customer.setContractEnd(form.contractEnd());
        customer.setRemark(form.remark());
        customer.setOwnerStaffId(form.ownerStaffId());
        return customer;
    }

    public record CustomerForm(
            @NotBlank(message = "客户姓名不能为空") String name,
            @NotBlank(message = "手机号不能为空") String phone,
            Integer gender,
            @NotBlank(message = "服务地址不能为空") String address,
            Long serviceItemId,
            String cycleDesc,
            LocalDate contractStart,
            LocalDate contractEnd,
            String remark,
            Long ownerStaffId) {
    }
}
