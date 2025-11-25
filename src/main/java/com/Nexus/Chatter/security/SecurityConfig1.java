//package com.Nexus.Chatter.security;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    private final TenantFilter tenantFilter;
//
//    public SecurityConfig(TenantFilter tenantFilter) {
//        this.tenantFilter = tenantFilter;
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
//                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for APIs
//                .authorizeHttpRequests(auth -> auth
//                        // Public Endpoints (The Widget)
//                        .requestMatchers("/api/v1/widget/**").permitAll()
//
//                        // Admin Endpoints (Require valid Clerk Token)
//                        .requestMatchers("/api/v1/admin/**", "/api/v1/tenants/**").authenticated()
//
//                        // Default
//                        .anyRequest().authenticated()
//                )
//                // Register Clerk as the OAuth2 Resource Server
//                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
//
//                // Add our custom Tenant Filter AFTER the Auth check
//                .addFilterAfter(tenantFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        // For development, allow localhost. In prod, this will be dynamic based on the Tenant URL.
//        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
//}