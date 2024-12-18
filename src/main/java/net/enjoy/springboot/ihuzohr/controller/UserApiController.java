package net.enjoy.springboot.ihuzohr.controller;

import net.enjoy.springboot.ihuzohr.entity.User;
import net.enjoy.springboot.ihuzohr.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserApiController {
    private static final Logger logger = LoggerFactory.getLogger(UserApiController.class);

    @Autowired
    private UserRepository userRepository;

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> updates, Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            // Update user fields
            if (updates.get("firstName") != null) {
                user.setFirstName((String) updates.get("firstName"));
            }
            if (updates.get("lastName") != null) {
                user.setLastName((String) updates.get("lastName"));
            }
            if (updates.get("phoneNumber") != null) {
                user.setPhoneNumber((String) updates.get("phoneNumber"));
            }
            if (updates.get("dateOfBirth") != null) {
                user.setDateOfBirth(java.time.LocalDate.parse((String) updates.get("dateOfBirth")));
            }

            user = userRepository.save(user);
            logger.info("Updated profile for user: {}", user.getEmail());

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Error updating profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update profile: " + e.getMessage());
        }
    }
}