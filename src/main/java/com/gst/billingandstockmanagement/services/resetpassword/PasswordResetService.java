package com.gst.billingandstockmanagement.services.resetpassword;

public interface PasswordResetService {
    void createAndSendResetToken(String email);
    boolean resetPassword(String token, String newPassword);
}
