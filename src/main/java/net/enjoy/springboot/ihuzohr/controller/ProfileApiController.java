package net.enjoy.springboot.ihuzohr.controller;

import net.enjoy.springboot.ihuzohr.entity.User;
import net.enjoy.springboot.ihuzohr.repository.UserRepository;
import net.enjoy.springboot.ihuzohr.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ProfileApiController {
    private static final Logger logger = LoggerFactory.getLogger(ProfileApiController.class);

    @Autowired
    private FileStorageService fileStorageService;

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

            // Save file and get filename only
            String savedFileName = fileStorageService.saveFile(file);
            // Extract just the filename if it's a full path
            String actualFilename = new File(savedFileName).getName();

            logger.info("File saved with name: {}", actualFilename);

            user.setProfilePicture(actualFilename); // Store only filename
            User savedUser = userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Profile picture updated successfully");
            response.put("profilePicture", actualFilename);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading profile picture", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload profile picture: " + e.getMessage());
        }
    }

    @GetMapping("/users/profile-picture/{filename:.+}")
    public ResponseEntity<Resource> getProfilePicture(@PathVariable String filename) {
        try {
            logger.info("Requesting profile picture: {}", filename);

            // Extract just the filename if it's a full path
            String actualFilename = new File(filename).getName();
            logger.info("Extracted filename: {}", actualFilename);

            var file = fileStorageService.getDownloadFile(actualFilename);

            // Log file details
            logger.info("Found file: exists={}, path={}, size={}",
                    file.exists(), file.getAbsolutePath(), file.length());

            String contentType = determineContentType(actualFilename);
            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + actualFilename + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error retrieving profile picture: {} - Error: {}", filename, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // Helper method to check current user's profile picture
    @GetMapping("/users/current-profile")
    public ResponseEntity<?> getCurrentUserProfile(Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            logger.info("Current user profile check - ID: {}, Email: {}, Profile Picture: {}",
                    user.getId(), user.getEmail(), user.getProfilePicture());
            return ResponseEntity.ok(Map.of("profilePicture", user.getProfilePicture()));
        } catch (Exception e) {
            logger.error("Error checking current user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String determineContentType(String filename) {
        String toLowerCase = filename.toLowerCase();
        if (toLowerCase.endsWith(".jpg") || toLowerCase.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (toLowerCase.endsWith(".png")) {
            return "image/png";
        } else if (toLowerCase.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream";
    }
}