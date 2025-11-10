package com.gst.billingandstockmanagement.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gst.billingandstockmanagement.entities.User;



@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findFirstByEmail(String email);
    
    Optional<User> findById(Long userId);
}
