package com.gst.billingandstockmanagement.services.user;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gst.billingandstockmanagement.dto.ChangePasswordDTO;
import com.gst.billingandstockmanagement.dto.SignupDTO;
import com.gst.billingandstockmanagement.dto.UpdateProfileDTO;
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
        // Manual BCrypt instantiation as per your pattern
        user.setPassword(new BCryptPasswordEncoder().encode(signupDTO.getPassword()));

        User createdUser = userRepository.save(user);

        // Map to DTO
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

    @Override
    public boolean changePassword(String email, ChangePasswordDTO changePasswordDTO) {
        User user = userRepository.findFirstByEmail(email);
        if (user == null) {
            return false;
        }

        // Use local encoder instance to verify old password
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (!encoder.matches(changePasswordDTO.getCurrentPassword(), user.getPassword())) {
            return false; // Incorrect old password
        }

        // Encode and save new password
        user.setPassword(encoder.encode(changePasswordDTO.getNewPassword()));
        userRepository.save(user);

        return true;
    }

    @Override
    public UserDTO getProfile(String email) {
        User user = userRepository.findFirstByEmail(email);
        if (user == null) return null;

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setFirstname(user.getFirstname());
        userDTO.setLastname(user.getLastname());
        userDTO.setEmail(user.getEmail());
        userDTO.setUserRole(user.getUserRole());
        return userDTO;
    }

    @Override
    public UserDTO updateProfile(String email, UpdateProfileDTO updateProfileDTO) {
        User user = userRepository.findFirstByEmail(email);
        if (user == null) return null;

        if (updateProfileDTO.getFirstname() != null) {
            user.setFirstname(updateProfileDTO.getFirstname());
        }

        if (updateProfileDTO.getLastname() != null) {
            user.setLastname(updateProfileDTO.getLastname());
        }

        User updatedUser = userRepository.save(user);

        // Map to DTO
        UserDTO userDTO = new UserDTO();
        userDTO.setId(updatedUser.getId());
        userDTO.setFirstname(updatedUser.getFirstname());
        userDTO.setLastname(updatedUser.getLastname());
        userDTO.setEmail(updatedUser.getEmail());
        userDTO.setUserRole(updatedUser.getUserRole());
        return userDTO;
    }
}