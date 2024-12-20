package net.enjoy.springboot.ihuzohr.controller;

import net.enjoy.springboot.ihuzohr.entity.User;
import net.enjoy.springboot.ihuzohr.repository.UserRepository;
import net.enjoy.springboot.ihuzohr.service.SupabaseStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "https://ihuzo-hr-with-react.vercel.app"}, allowCredentials = "true")
public class ProfileApiController {
    private static final Logger logger = LoggerFactory.getLogger(ProfileApiController.class);

    @Autowired
    private SupabaseStorageService storageService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/users/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(
            @RequestPart("file") MultipartFile file,
            Principal principal) {
        try {
            logger.info("Received profile picture upload request for user: {}", principal.getName());

            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                logger.error("User not found for email: {}", principal.getName());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            // Delete old profile picture if exists
            if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
                try {
                    storageService.deleteFile(user.getProfilePicture());
                } catch (Exception e) {
                    logger.warn("Failed to delete old profile picture: {}", e.getMessage());
                    // Continue with upload even if delete fails
                }
            }

            // Upload new file and get URL
            String fileUrl = storageService.uploadFile(file);
            logger.info("File uploaded successfully. URL: {}", fileUrl);

            user.setProfilePicture(fileUrl);
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Profile picture updated successfully");
            response.put("profilePicture", fileUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading profile picture", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload profile picture: " + e.getMessage()));
        }
    }

    // No need for getProfilePicture endpoint as we're using direct Supabase URLs

    @GetMapping("/users/current-profile")
    public ResponseEntity<?> getCurrentUserProfile(Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            logger.info("Current user profile check - ID: {}, Email: {}, Profile Picture: {}",
                    user.getId(), user.getEmail(), user.getProfilePicture());

            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "profilePicture", user.getProfilePicture() != null ? user.getProfilePicture() : "",
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName()
            ));
        } catch (Exception e) {
            logger.error("Error checking current user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving user profile"));
        }
    }
}