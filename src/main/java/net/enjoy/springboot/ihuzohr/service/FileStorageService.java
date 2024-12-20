package net.enjoy.springboot.ihuzohr.service;

import org.springframework.beans.factory.annotation.Value;
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
    private final String storageDirectory;

    public FileStorageService(@Value("${file.upload-dir:#{systemProperties['java.io.tmpdir']}/uploads}") String storageDirectory) {
        this.storageDirectory = storageDirectory;
        initializeStorageDirectory();
    }

    private void initializeStorageDirectory() {
        try {
            File directory = new File(storageDirectory);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            System.out.println("Storage directory initialized at: " + directory.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    public String saveFile(MultipartFile fileToSave) throws IOException {
        if (fileToSave == null || fileToSave.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        // Generate filename with timestamp
        String originalFilename = fileToSave.getOriginalFilename();
        String filename = System.currentTimeMillis() + "_" + originalFilename;
        Path targetLocation = Paths.get(storageDirectory).resolve(filename);

        Files.copy(fileToSave.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        return filename; // Return only filename
    }

    public File getDownloadFile(String fileName) throws IOException {
        Path filePath = Paths.get(storageDirectory).resolve(fileName).normalize();
        File file = filePath.toFile();

        if (!file.exists()) {
            throw new IOException("File not found: " + fileName);
        }

        // Security check
        if (!file.getCanonicalPath().startsWith(new File(storageDirectory).getCanonicalPath())) {
            throw new IOException("Access denied");
        }

        return file;
    }
}