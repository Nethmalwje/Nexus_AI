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
@Table(name = "tenants")
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "website_domain")
    private String websiteDomain;

    @Column(name = "plan_tier")
    private String planTier = "FREE";

    @Column(name = "clerk_org_id")
    private String clerkOrgId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}