package com.gst.billingandstockmanagement.controllers;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gst.billingandstockmanagement.dto.ChangePasswordDTO;
import com.gst.billingandstockmanagement.dto.UpdateProfileDTO;
import com.gst.billingandstockmanagement.dto.UserDTO;
import com.gst.billingandstockmanagement.services.user.UserService;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO, Principal principal) {
        // Principal guarantees we are modifying the currently logged-in user.
        // principal.getName() returns the email used for authentication.
        boolean success = userService.changePassword(principal.getName(), changePasswordDTO);

        if (success) {
            return ResponseEntity.ok().build();
        } else {
            // This usually indicates the currentPassword sent by the user was incorrect.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect current password");
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        // Fetch the profile for the currently authenticated user.
        UserDTO userDTO = userService.getProfile(principal.getName());
        if (userDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        return ResponseEntity.ok(userDTO);
    }

    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileDTO updateProfileDTO, Principal principal) {
        // Update the profile for the currently authenticated user.
        UserDTO updatedUser = userService.updateProfile(principal.getName(), updateProfileDTO);
        if (updatedUser == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Update failed");
        }
        return ResponseEntity.ok(updatedUser);
    }
}