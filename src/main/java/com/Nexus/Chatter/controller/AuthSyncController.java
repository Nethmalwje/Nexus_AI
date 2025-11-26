package com.Nexus.Chatter.controller;

import com.Nexus.Chatter.model.AppUser;
import com.Nexus.Chatter.model.Tenant;
import com.Nexus.Chatter.repo.AppUserRepo;
import com.Nexus.Chatter.repo.TenantRepo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthSyncController {

    private final AppUserRepo appUserRepository;
    private final TenantRepo tenantRepository;

    // Hardcoded Dev tenant ID
    private static final UUID DEV_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    public AuthSyncController(AppUserRepo appUserRepository,
                              TenantRepo tenantRepository) {
        this.appUserRepository = appUserRepository;
        this.tenantRepository = tenantRepository;
    }

    @PostMapping("/sync")
    public AppUser syncUser(@AuthenticationPrincipal Jwt jwt) {

        // 1. Extract data from JWT
        String clerkUserId = jwt.getClaim("user_id");    // from your Clerk template
        String email = jwt.getClaim("email");

        // 2. Load DEV tenant
        Tenant devTenant = tenantRepository.findById(DEV_TENANT_ID)
                .orElseThrow(() -> new IllegalStateException("Dev tenant not found"));

        // 3. If user exists → update email + return
        return appUserRepository.findById(clerkUserId)
                .map(existing -> {
                    // update email if changed
                    if (email != null && !email.equals(existing.getEmail())) {
                        existing.setEmail(email);
                    }
                    // ensure tenant is set (in case old data is missing)
                    if (existing.getTenant() == null) {
                        existing.setTenant(devTenant);
                    }
                    return appUserRepository.save(existing);
                })
                .orElseGet(() -> {
                    // 4. Create new AppUser
                    AppUser newUser = new AppUser();
                    newUser.setId(clerkUserId);
                    newUser.setEmail(email);
                    newUser.setTenant(devTenant);       // 🔥 Assign tenant entity
                    newUser.setRole("ADMIN");           // default
                    return appUserRepository.save(newUser);
                });
    }
}
