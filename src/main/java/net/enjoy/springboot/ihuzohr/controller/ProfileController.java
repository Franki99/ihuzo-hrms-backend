package net.enjoy.springboot.ihuzohr.controller;

import net.enjoy.springboot.ihuzohr.repository.UserRepository;
import net.enjoy.springboot.ihuzohr.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
public class ProfileController {
    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/uploadProfilePicture")
    public String handleFileUpload(@RequestParam("profilePicture") MultipartFile file, @RequestParam(value = "userId") Long userId, RedirectAttributes redirectAttributes) {
        try {
           String savedFile = fileStorageService.saveFile(file);
           userRepository.findById(userId).ifPresent(user -> {
               user.setProfilePicture(savedFile);
               userRepository.save(user);
              });
            redirectAttributes.addFlashAttribute("messageSuccess", "Profile picture updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("messageError", "Failed to upload profile picture!");
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/download-profile")
    public ResponseEntity<Resource> downloadFileFaster(@RequestParam("fileName") String filename) {
        try {
            var fileToDownload = fileStorageService.getDownloadFile(filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentLength(fileToDownload.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new FileSystemResource(fileToDownload));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
