package com.example.service.saas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 门店 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "store")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantId;

    private String name;

    private String address;

    private String phone;

    private Integer status;

    private LocalDateTime createdAt;
}
