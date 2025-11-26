package com.Nexus.Chatter.security;

import com.Nexus.Chatter.model.AppUser;
import com.Nexus.Chatter.repo.AppUserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private final AppUserRepo appUserRepository;

    public TenantFilter(AppUserRepo appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Skip checks for the sync endpoint (user registration)
        if (path.startsWith("/api/auth/sync")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {

            String clerkUserId = null;

            // 🔹 Try to extract from JWT if this is a JWT auth
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = (Jwt) jwtAuth.getPrincipal();

                // Prefer the same claim you used in AuthSyncController
                clerkUserId = jwt.getClaim("user_id");

                // Fallback to sub if user_id missing (they're same in your case)
                if (clerkUserId == null) {
                    clerkUserId = jwt.getSubject(); // "sub"
                }
            } else {
                // Fallback: use the name (often mapped to sub)
                clerkUserId = authentication.getName();
            }

            if (clerkUserId == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("""
                    {
                      "error": "MISSING_USER_ID",
                      "message": "Could not resolve user id from JWT."
                    }
                    """);
                return;
            }

            // 🔹 Check DB: does this AppUser exist?
            Optional<AppUser> userOptional = appUserRepository.findById(clerkUserId);

            if (userOptional.isEmpty()) {
                // JWT is valid but user is not in our DB
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("""
                    {
                      "error": "APP_USER_NOT_REGISTERED",
                      "message": "User is authenticated with Clerk but not registered in Nexus backend. Call /api/auth/sync first."
                    }
                    """);
                return;
            }

            AppUser user = userOptional.get();

            // 🔹 Put tenant into ThreadLocal if present
            if (user.getTenant() != null && user.getTenant().getId() != null) {
                UUID tenantId = user.getTenant().getId();
                TenantContext.setTenantId(tenantId);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
