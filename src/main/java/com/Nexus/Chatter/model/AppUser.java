package com.Nexus.Chatter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "app_users")
public class AppUser {
    @Id
    private String id; // This is the Clerk User ID (String, not UUID)

    @Column(nullable = false)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    private String role = "ADMIN";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}