package com.kushwahacafe.cafe.controller;

import com.kushwahacafe.cafe.model.User;
import com.kushwahacafe.cafe.repository.UserRepository;
import com.kushwahacafe.cafe.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    // --- VIEW: Request Name & Gmail ---
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("user_id") != null) {
            return "redirect:/";
        }
        return "login";
    }

    // --- POST: Generate OTP & Send Email ---
    @PostMapping("/login/send-otp")
    public String sendOtp(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (username == null || username.trim().isEmpty() ||
            email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please fill in all fields.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/login";
        }

        username = username.trim();
        email = email.trim().toLowerCase();

        // 1. Generate 6-Digit OTP
        String otpCode = String.format("%06d", 100000 + new Random().nextInt(900000));
        System.out.println("[OTP DEBUG] Generated OTP: " + otpCode + " for email: " + email);

        // 2. Save OTP details in session (expires in 5 minutes)
        session.setAttribute("otp_code", otpCode);
        session.setAttribute("otp_email", email);
        session.setAttribute("otp_username", username);
        session.setAttribute("otp_expiry", LocalDateTime.now().plusMinutes(5));

        // 3. Send Email Notification
        String otpMailHtml = String.format(
            "<div style=\"font-family: Arial, sans-serif; border: 1px solid #e2e8f0; padding: 20px; border-radius: 8px; max-width: 600px; background-color: #FCF8F2;\">" +
            "    <h2 style=\"color: #2A1A10; border-bottom: 2px solid #D4AF37; padding-bottom: 10px; text-align: center;\">Kushwaha Cafe OTP Verification Code</h2>" +
            "    <p>Dear <b>%s</b>,</p>" +
            "    <p>Your one-time passcode (OTP) for secure login is:</p>" +
            "    <div style=\"text-align: center; margin: 2rem 0;\">" +
            "        <span style=\"font-size: 2.2rem; font-weight: 800; letter-spacing: 5px; color: #2A1A10; background: #FFF5E6; padding: 0.8rem 2rem; border-radius: 6px; border: 1px dashed var(--accent);\">%s</span>" +
            "    </div>" +
            "    <p style=\"color: #718096; font-size: 0.85rem; text-align: center;\">This verification code is valid for 5 minutes. Please do not share it with anyone.</p>" +
            "    <br>" +
            "    <p>Warm Regards,</p>" +
            "    <p><b>Kushwaha Cafe Team</b></p>" +
            "</div>",
            username, otpCode
        );

        emailService.sendEmailNotification(email, "Login Verification Code - Kushwaha Cafe", otpMailHtml);

        redirectAttributes.addFlashAttribute("flashMessage", "Verification OTP sent to " + email);
        redirectAttributes.addFlashAttribute("flashCategory", "success");

        return "redirect:/login/verify";
    }

    // --- VIEW: Enter OTP Code ---
    @GetMapping("/login/verify")
    public String verifyOtpPage(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("user_id") != null) {
            return "redirect:/";
        }
        if (session.getAttribute("otp_code") == null) {
            redirectAttributes.addFlashAttribute("flashMessage", "Session expired. Please enter details again.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/login";
        }
        return "verify_otp";
    }

    // --- POST: Verify OTP & Authenticate ---
    @PostMapping("/login/verify")
    public String verifyOtp(
            @RequestParam("otp") String enteredOtp,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("user_id") != null) {
            return "redirect:/";
        }

        String sessionOtp = (String) session.getAttribute("otp_code");
        String otpEmail = (String) session.getAttribute("otp_email");
        String otpUsername = (String) session.getAttribute("otp_username");
        LocalDateTime otpExpiry = (LocalDateTime) session.getAttribute("otp_expiry");

        if (sessionOtp == null || otpEmail == null || otpUsername == null || otpExpiry == null) {
            redirectAttributes.addFlashAttribute("flashMessage", "Session expired. Please request a new OTP.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/login";
        }

        if (LocalDateTime.now().isAfter(otpExpiry)) {
            redirectAttributes.addFlashAttribute("flashMessage", "OTP has expired. Please request a new one.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            clearOtpSession(session);
            return "redirect:/login";
        }

        if (!sessionOtp.equals(enteredOtp.trim())) {
            redirectAttributes.addFlashAttribute("flashMessage", "Invalid verification code. Please try again.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/login/verify";
        }

        // OTP is correct - Authenticate / Register
        Optional<User> userOpt = userRepository.findByEmail(otpEmail);
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            // New user registration - Check if username is already taken
            String chosenUsername = otpUsername;
            if (userRepository.findByUsername(chosenUsername).isPresent()) {
                chosenUsername = chosenUsername + "_" + String.format("%04d", new java.util.Random().nextInt(10000));
            }

            user = new User();
            user.setUsername(chosenUsername);
            user.setEmail(otpEmail);
            user.setPasswordHash("OTP_USER"); // dummy password hash to bypass DB column constraints
            user.setRole("customer");
            userRepository.save(user);

            // Send Welcome Email
            String welcomeHtml = String.format(
                "<h3>Welcome to Kushwaha Cafe, %s!</h3>" +
                "<p>Thank you for registering an account with us.</p>" +
                "<p>You can now book tables, order delicious coffee and bakery items, and view your orders from your profile.</p>" +
                "<br>" +
                "<p>Warm Regards,</p>" +
                "<p><b>Kushwaha Cafe Team</b></p>",
                otpUsername
            );
            emailService.sendEmailNotification(otpEmail, "Welcome to Kushwaha Cafe!", welcomeHtml);
        }

        // Set standard session attributes
        session.setAttribute("user_id", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("email", user.getEmail());
        session.setAttribute("role", user.getRole());

        // Clear OTP attributes from session
        clearOtpSession(session);

        redirectAttributes.addFlashAttribute("flashMessage", "Welcome back, " + user.getUsername() + "!");
        redirectAttributes.addFlashAttribute("flashCategory", "success");

        if ("admin".equals(user.getRole())) {
            return "redirect:/admin";
        }
        return "redirect:/";
    }

    private void clearOtpSession(HttpSession session) {
        session.removeAttribute("otp_code");
        session.removeAttribute("otp_email");
        session.removeAttribute("otp_username");
        session.removeAttribute("otp_expiry");
    }

    // --- GET: Logout ---
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("flashMessage", "Logged out successfully.");
        redirectAttributes.addFlashAttribute("flashCategory", "success");
        return "redirect:/";
    }
}
