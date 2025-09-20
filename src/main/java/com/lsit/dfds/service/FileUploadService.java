package com.lsit.dfds.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService implements IFileUploadService {

	@Override
	public String uploadFile(String filePath, String fileName, MultipartFile file) throws IOException {
		// Ensure the provided filePath directory exists
		String orgfileName = file.getOriginalFilename();
		if(!orgfileName.matches("^[a-zA-Z0-9-_]+\\.(pdf)$")) {
		    throw new IllegalArgumentException("Invalid file type");
		}
		String mimeType = file.getContentType();
		if(!java.util.List.of("application/pdf", "application/msword", 
		            "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
		            .contains(mimeType)) {
		    throw new IllegalArgumentException("Invalid MIME type: " + mimeType);
		}
		Path directoryPath = Paths.get(filePath);
		if (!Files.exists(directoryPath)) {
			Files.createDirectories(directoryPath); // Create directories if they do not exist
		}

		// Combine file path and file name
		Path fileDestination = directoryPath.resolve(fileName);

		try {
			// Copy the uploaded file to the destination path
			Files.copy(file.getInputStream(), fileDestination, StandardCopyOption.REPLACE_EXISTING);
			return fileDestination.toString(); // Return the file path or URL
		} catch (IOException e) {
			throw new RuntimeException("File upload failed", e);
		}
	}
}
