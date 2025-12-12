//package com.Nexus.Chatter.security;
//
//import com.Nexus.Chatter.model.AppUser;
//import com.Nexus.Chatter.repo.AppUserRepo;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.Optional;
//import java.util.UUID;
//
//@Component
//public class TenantFilter extends OncePerRequestFilter {
//
//    private final AppUserRepo appUserRepository;
//
//    public TenantFilter(AppUserRepo appUserRepository) {
//        this.appUserRepository = appUserRepository;
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws ServletException, IOException {
//
//        String path = request.getRequestURI();
//
//        // 🔹 1. Skip tenant resolution for the sync endpoint
//        // This endpoint is used to CREATE the AppUser, so DB may not have the user yet.
//        if (path.startsWith("/api/auth/sync")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        // 2. Get the authenticated user from Spring Security
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        if (authentication != null && authentication.isAuthenticated()) {
//            // Clerk uses "sub" as the user ID, Spring maps this to authentication.getName()
//            String clerkUserId = authentication.getName();
//
//            // 3. Find the user in our DB to get their Tenant ID
//            Optional<AppUser> userOptional = appUserRepository.findById(clerkUserId);
//
//            if (userOptional.isPresent()) {
//                AppUser user = userOptional.get();
//
//                if (user.getTenant() != null && user.getTenant().getId() != null) {
//                    UUID tenantId = user.getTenant().getId();
//                    TenantContext.setTenantId(tenantId);
//                }
//            }
//        }
//
//        try {
//            // Let the request proceed
//            filterChain.doFilter(request, response);
//        } finally {
//            // 4. Always clear ThreadLocal to avoid leaks
//            TenantContext.clear();
//        }
//    }
//}
