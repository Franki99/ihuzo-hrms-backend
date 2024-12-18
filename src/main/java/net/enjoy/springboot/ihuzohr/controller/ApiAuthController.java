package net.enjoy.springboot.ihuzohr.controller;

import net.enjoy.springboot.ihuzohr.dto.LoginRequest;
import net.enjoy.springboot.ihuzohr.entity.User;
import net.enjoy.springboot.ihuzohr.entity.Role;
import net.enjoy.springboot.ihuzohr.dto.UserDto;
import net.enjoy.springboot.ihuzohr.service.UserService;
import net.enjoy.springboot.ihuzohr.config.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import net.enjoy.springboot.ihuzohr.repository.UserRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import net.enjoy.springboot.ihuzohr.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {
    private static final Logger logger = LoggerFactory.getLogger(ApiAuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for email: {}", loginRequest.getEmail());
        try {
            User user = userService.findUserByEmail(loginRequest.getEmail());

            if (user == null) {
                logger.warn("User not found for email: {}", loginRequest.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid credentials");
            }

            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                logger.warn("Invalid password for email: {}", loginRequest.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid credentials");
            }

            String token = jwtTokenUtil.generateToken(user);

            // Create a simplified response without circular references
            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("id", user.getId());
            userResponse.put("email", user.getEmail());
            userResponse.put("firstName", user.getFirstName());
            userResponse.put("lastName", user.getLastName());
            userResponse.put("profilePicture", user.getProfilePicture());
            // Only send role names
            userResponse.put("roles", user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toList()));

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", userResponse);

            logger.info("Login successful for user: {}", user.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Login error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during login");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto userDto) {
        try {
            User existingUser = userService.findUserByEmail(userDto.getEmail());
            if (existingUser != null) {
                return ResponseEntity.badRequest()
                        .body("There is already an account registered with this email");
            }

            // Use saveUser to create a normal user (not admin)
            userService.saveUser(userDto);

            return ResponseEntity.ok()
                    .body("Registration successful");
        } catch (Exception e) {
            logger.error("Registration error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Registration failed: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        try {
            if (principal == null) {
                logger.warn("Unauthorized access attempt to /me endpoint");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Not authenticated");
            }

            User user = userService.findUserByEmail(principal.getName());
            if (user == null) {
                logger.error("User not found for principal: {}", principal.getName());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }

            // Create response with only necessary user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("email", user.getEmail());
            userData.put("firstName", user.getFirstName());
            userData.put("lastName", user.getLastName());
            userData.put("profilePicture", user.getProfilePicture());
            userData.put("roles", user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()));

            logger.info("Current user info retrieved for: {}", principal.getName());
            return ResponseEntity.ok(userData);

        } catch (Exception e) {
            logger.error("Error retrieving current user: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving user information");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Principal principal) {
        try {
            if (principal != null) {
                logger.info("User logged out: {}", principal.getName());
            }
            return ResponseEntity.ok()
                    .body("Logged out successfully");
        } catch (Exception e) {
            logger.error("Error during logout: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during logout");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            logger.info("Received forgot password request for email: {}", email);

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Email is required");
            }

            User user = userService.findUserByEmail(email);
            if (user == null) {
                logger.warn("No user found with email: {}", email);
                return ResponseEntity.ok()
                        .body("If your email exists in our system, you will receive password reset instructions");
            }

            // Create context for email template
            Context context = new Context();
            context.setVariable("recipient", user.getFirstName() + " " + user.getLastName());
            String resetLink = "http://localhost:5173/reset-password?email=" + user.getEmail();
            context.setVariable("resetLink", resetLink);
            context.setVariable("message", "Please click the button below to reset your password:");

            // Send email using EmailService
            emailService.sendEmail(
                    user.getEmail(),
                    "Password Reset Request",
                    context
            );

            logger.info("Password reset email sent to: {}", email);
            return ResponseEntity.ok()
                    .body("Password reset instructions sent");

        } catch (Exception e) {
            logger.error("Error in forgot password process: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing password reset request: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            logger.info("Received password reset request for email: {}", email);

            if (email == null || password == null) {
                return ResponseEntity.badRequest()
                        .body("Email and password are required");
            }

            User user = userService.findUserByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body("User not found");
            }

            // Update user's password
            user.setPassword(passwordEncoder.encode(password));
            userService.updateUser(convertToUserDto(user));

            logger.info("Password reset successful for user: {}", email);
            return ResponseEntity.ok()
                    .body("Password reset successfully");

        } catch (Exception e) {
            logger.error("Error in password reset: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error resetting password: " + e.getMessage());
        }
    }

    private UserDto convertToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setDateOfBirth(user.getDateOfBirth());
        return dto;
    }


}