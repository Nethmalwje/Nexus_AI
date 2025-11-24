package com.Nexus.Chatter.service;

import com.Nexus.Chatter.model.AppUser;
import com.Nexus.Chatter.repo.AppUserRepo;
import org.springframework.beans.factory.annotation.Autowired;

public class User {
    //id ="0000021"
    @Autowired
    private AppUserRepo userRepo;

    public AppUser getUserById(String id){
        return userRepo.findById(id).orElse(null);
    }
}
