package com.gst.billingandstockmanagement.services.user;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gst.billingandstockmanagement.dto.SignupDTO;
import com.gst.billingandstockmanagement.dto.UserDTO;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.enums.UserRole;
import com.gst.billingandstockmanagement.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;


    @Override
    @Transactional
    public UserDTO createUser(SignupDTO signupDTO) {
        User user = new User();
        user.setFirstname(signupDTO.getFirstname());
        user.setLastname(signupDTO.getLastname());
        user.setEmail(signupDTO.getEmail());
        // Check if userRole is provided, otherwise set default value as "USER"
        UserRole userRole = signupDTO.getUserRole() != null ? signupDTO.getUserRole() : UserRole.USER;
        user.setUserRole(userRole);
        user.setPassword(new BCryptPasswordEncoder().encode(signupDTO.getPassword()));
        User createdUser = userRepository.save(user);
        UserDTO userDTO = new UserDTO();
        userDTO.setId(createdUser.getId());
        userDTO.setFirstname(createdUser.getFirstname());
        userDTO.setLastname(createdUser.getLastname());
        userDTO.setEmail(createdUser.getEmail());
        userDTO.setUserRole(createdUser.getUserRole());
        return userDTO;
    }

    @Override
    public boolean hasUserWithEmail(String email) {
        return userRepository.findFirstByEmail(email) != null;
    }

	@Override
	public User getUserById(Long userId) {
		Optional<User> userOptional = userRepository.findById(userId);
	    return userOptional.orElse(null);
	}

}
