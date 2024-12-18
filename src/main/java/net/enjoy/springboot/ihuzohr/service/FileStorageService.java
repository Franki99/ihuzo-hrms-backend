package net.enjoy.springboot.ihuzohr.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {
    public static final String STORAGE_DIRECTORY = "C:\\Users\\hp\\Documents\\pictures";

    public FileStorageService() {
        // Create directory if it doesn't exist
        File directory = new File(STORAGE_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public String saveFile(MultipartFile fileToSave) throws IOException {
        if (fileToSave == null || fileToSave.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        // Generate filename with timestamp
        String originalFilename = fileToSave.getOriginalFilename();
        String filename = System.currentTimeMillis() + "_" + originalFilename;
        Path targetLocation = Paths.get(STORAGE_DIRECTORY).resolve(filename);

        Files.copy(fileToSave.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        return filename; // Return only filename, not full path
    }

    public File getDownloadFile(String fileName) throws IOException {
        Path filePath = Paths.get(STORAGE_DIRECTORY).resolve(fileName).normalize();
        File file = filePath.toFile();

        if (!file.exists()) {
            throw new IOException("File not found: " + fileName);
        }

        // Security check
        if (!file.getCanonicalPath().startsWith(new File(STORAGE_DIRECTORY).getCanonicalPath())) {
            throw new IOException("Access denied");
        }

        return file;
    }
}