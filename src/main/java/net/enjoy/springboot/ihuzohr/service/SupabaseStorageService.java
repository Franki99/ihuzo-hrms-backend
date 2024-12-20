package net.enjoy.springboot.ihuzohr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket.name}")
    private String bucketName;

    private final RestTemplate restTemplate = new RestTemplate();

    public String uploadFile(MultipartFile file) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String storageUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.setContentType(MediaType.valueOf(file.getContentType()));

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

            restTemplate.put(storageUrl, requestEntity);

            // Construct and return the public URL
            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;

        } catch (Exception e) {
            log.error("Error uploading file to Supabase Storage", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);

            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            restTemplate.delete(deleteUrl, requestEntity);
        } catch (Exception e) {
            log.error("Error deleting file from Supabase Storage", e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }
}