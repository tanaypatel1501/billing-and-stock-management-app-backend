package com.gst.billingandstockmanagement.services.jwt;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findFirstByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("Username not found: " + username);
        }

        // Extract the role string (handles both Enum and String types safely)
        String roleName = user.getUserRole() != null ? user.getUserRole().toString() : "USER";

        // Create the authority wrapper.
        // We prepend "ROLE_" so that @PreAuthorize("hasRole('ADMIN')") works correctly out of the box.
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + roleName);

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(authority)
        );
    }
}