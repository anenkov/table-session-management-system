package com.nenkov.bar.user;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("app_user")
public record UserEntity(
    @Id UUID id,
    @Column("username") String username,
    @Column("password_hash") String passwordHash,
    @Column("role") String role, // e.g. "ROLE_USER", "ROLE_MANAGER"
    @Column("is_active") boolean active,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt) {}
