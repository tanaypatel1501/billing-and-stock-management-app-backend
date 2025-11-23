package com.gst.billingandstockmanagement.services.user;

import com.gst.billingandstockmanagement.dto.SignupDTO;
import com.gst.billingandstockmanagement.dto.UpdateProfileDTO;
import com.gst.billingandstockmanagement.dto.UserDTO;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.dto.ChangePasswordDTO;

public interface UserService {
    UserDTO createUser(SignupDTO signupDTO);

    boolean hasUserWithEmail(String email);

	User getUserById(Long userId);

    boolean changePassword(String email, ChangePasswordDTO changePasswordDTO);

    UserDTO getProfile(String email);

    UserDTO updateProfile(String email, UpdateProfileDTO updateProfileDTO);
}

