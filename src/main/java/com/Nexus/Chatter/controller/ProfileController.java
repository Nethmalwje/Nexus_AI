//package com.Nexus.Chatter.controller;
//
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//public class ProfileController {
//
//    @GetMapping("/api/profile")
//    public Map<String, Object> profile(@AuthenticationPrincipal Jwt jwt) {
//
//        Map<String, Object> profile = new HashMap<>();
//        profile.put("id", jwt.getClaim("sub"));
//        profile.put("email", jwt.getClaim("email"));
//        profile.put("firstName", jwt.getClaim("first_name"));
//        profile.put("lastName", jwt.getClaim("last_name"));
//
//        return profile;
//    }
//}