package com.gst.billingandstockmanagement.services.user;

import com.gst.billingandstockmanagement.dto.SignupDTO;
import com.gst.billingandstockmanagement.dto.UserDTO;
import com.gst.billingandstockmanagement.entities.User;

public interface UserService {
    UserDTO createUser(SignupDTO signupDTO);

    boolean hasUserWithEmail(String email);

	User getUserById(Long userId);
}

