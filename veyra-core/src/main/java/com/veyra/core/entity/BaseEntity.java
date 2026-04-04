package com.veyra.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Tüm entity'lerin extend ettiği temel sınıf.
 * Ortak alanları (id, audit tarihleri, soft delete) tek bir yerde tutar — SRP.
 * @EnableJpaAuditing VeyraApplication'da aktif edilmelidir.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Soft delete — kayıt veritabanından fiziksel olarak silinmez
    @Column(nullable = false)
    private boolean deleted = false;
}
