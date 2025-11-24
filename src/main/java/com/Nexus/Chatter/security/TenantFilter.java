//package com.Nexus.Chatter.security;
//
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
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//
//        // 1. Get the authenticated user from Spring Security
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        if (authentication != null && authentication.isAuthenticated()) {
//            // Clerk uses the "sub" claim as the User ID (e.g., user_2bxf...)
//            String clerkUserId = authentication.getName();
//
//            // 2. Find the user in our DB to get their Tenant ID
//            Optional<AppUser> userOptional = appUserRepository.findById(clerkUserId);
//
//            if (userOptional.isPresent()) {
//                TenantContext.setTenantId(userOptional.get().getTenant().getId());
//            }
//        }
//
//        try {
//            filterChain.doFilter(request, response);
//        } finally {
//            // 3. CLEAN UP! Critical to prevent memory leaks in thread pools
//            TenantContext.clear();
//        }
//    }
//}