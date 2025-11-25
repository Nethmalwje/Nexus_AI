package com.Nexus.Chatter.controller;

import com.Nexus.Chatter.model.AppUser;
import com.Nexus.Chatter.repo.AppUserRepo;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthSyncController {

    private final AppUserRepo appUserRepository;

    public AuthSyncController(AppUserRepo appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @PostMapping("/sync")
    public AppUser syncUser(@AuthenticationPrincipal Jwt jwt) {
        // 1. Get data from JWT
        String clerkUserId = jwt.getClaim("user_id");    // "sub"
        String email = jwt.getClaim("email");     // "email" claim

        // 2. If already exists, return existing user
        return appUserRepository.findById(clerkUserId)
                .orElseGet(() -> {
                    // 3. If not, create new AppUser
                    AppUser user = new AppUser();
                    user.setId(clerkUserId);
                    user.setEmail(email);
                    user.setTenant(null);      // we'll do proper tenant later
                    user.setRole("ADMIN");     // default for now

                    return appUserRepository.save(user);
                });
    }
}
