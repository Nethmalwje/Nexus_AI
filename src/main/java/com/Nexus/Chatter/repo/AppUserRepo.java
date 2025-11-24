package com.Nexus.Chatter.repo;


import com.Nexus.Chatter.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface AppUserRepo extends JpaRepository<AppUser, String> {}

