package com.gst.billingandstockmanagement.utils;

public class EmailTemplates {

    private EmailTemplates() {}

    public static String verificationEmail(String firstname, String verifyLink) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f7f6;font-family:sans-serif;'>"
                + "<table width='100%' cellspacing='0' cellpadding='0'><tr><td align='center' style='padding:40px 10px;'>"
                + "<div style='max-width:600px;background:#fff;border-radius:16px;overflow:hidden;border:1px solid #e1e8e5;'>"
                + "<table width='100%'><tr><td bgcolor='#121212' align='center' style='padding:40px 20px;'>"
                + "<h1 style='margin:0;font-size:28px;color:#fff;'>GST <span style='color:#48e3cc;'>Medicose</span></h1></td></tr></table>"
                + "<div style='padding:40px 30px;'>"
                + "<h2 style='color:#000;font-size:22px;'>Verify your email address</h2>"
                + "<p style='color:#333;font-size:15px;'>Hello " + firstname + ",</p>"
                + "<p style='color:#333;font-size:15px;'>Click the button below to verify your email address. This link expires in 24 hours.</p>"
                + "<table width='100%' style='margin:30px 0;'><tr><td align='center'>"
                + "<a href='" + verifyLink + "' style='display:inline-block;background:linear-gradient(135deg,#48e3cc 0%,#36c5b0 100%);"
                + "color:#000;padding:16px 40px;text-decoration:none;border-radius:12px;font-weight:700;font-size:16px;'>Verify Email Address</a>"
                + "</td></tr></table>"
                + "<p style='color:#999;font-size:13px;'>If you didn't create this account, you can safely ignore this email.</p>"
                + "</div></div></td></tr></table></body></html>";
    }
}