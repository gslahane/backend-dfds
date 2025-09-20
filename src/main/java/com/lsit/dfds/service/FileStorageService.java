// src/main/java/com/lsit/dfds/storage/FileStorageService.java
package com.lsit.dfds.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

	// Base directory for uploads. Override via application.properties:
	// app.upload.root=/var/dfds/uploads
	@Value("${app.upload.root:uploads}")
	private String uploadRoot;

	// Public URL base to serve files from. Must match the ResourceHandler mapping
	// below.
	@Value("${app.upload.public-base:/files}")
	private String publicBase;

	private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "jpg", "jpeg", "png");

	public String storeRecommendationLetter(MultipartFile file) {
		return store(file, "recommendations");
	}

	private String store(MultipartFile file, String subfolder) {
		if (file == null || file.isEmpty()) {
			return null;
		}

		String original = file.getOriginalFilename();
		String safeName = sanitizeFilename(original);
		String ext = getExt(safeName);
		if (!ALLOWED_EXT.contains(ext.toLowerCase())) {
			throw new RuntimeException("Unsupported file type: " + ext);
		}

		try {
			Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
			Path folder = root.resolve(subfolder);
			Files.createDirectories(folder);

			String unique = UUID.randomUUID().toString() + "_" + safeName;
			Path dest = folder.resolve(unique).normalize();

			// Basic path traversal guard
			if (!dest.startsWith(folder)) {
				throw new RuntimeException("Invalid path");
			}

			Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

			// Return a URL that maps to ResourceHandler (see config below)
			String url = publicBase + "/" + subfolder + "/" + unique;
			return url;
		} catch (IOException ex) {
			throw new RuntimeException("Failed to store file", ex);
		}
	}

	private String sanitizeFilename(String name) {
		if (name == null) {
			name = "file";
		}
		// strip directories and weird chars
		name = name.replace("\\", "/");
		int slash = name.lastIndexOf('/');
		if (slash >= 0) {
			name = name.substring(slash + 1);
		}
		// allow simple chars
		return name.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

	private String getExt(String name) {
		int dot = name.lastIndexOf('.');
		return (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
	}

	/**
	 * Saves a file to the local filesystem under the given subfolder and returns a
	 * public URL. Example: saveFile(file, "aa-letters") ->
	 * /files/aa-letters/<uuid>_original.ext
	 *
	 * @param aaLetterFile The uploaded file (MultipartFile)
	 * @param subfolder    The storage subfolder (e.g., "recommendations",
	 *                     "aa-letters")
	 * @return Public URL string to store in DB (or null if file is null/empty)
	 */
	public String saveFile(MultipartFile aaLetterFile, String subfolder) {
		if (aaLetterFile == null || aaLetterFile.isEmpty()) {
			return null;
		}
		String fileName = aaLetterFile.getOriginalFilename();
		if(!fileName.matches("^[a-zA-Z0-9-_]+\\.(pdf)$")) {
		    throw new IllegalArgumentException("Invalid file type");
		}
		String mimeType = aaLetterFile.getContentType();
		if(!java.util.List.of("application/pdf", "application/msword", 
		            "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
		            .contains(mimeType)) {
		    throw new IllegalArgumentException("Invalid MIME type: " + mimeType);
		}
		if (subfolder == null || subfolder.isBlank()) {
			subfolder = "misc";
		}

		// Sanitize filename and validate extension
		String original = Objects.requireNonNullElse(aaLetterFile.getOriginalFilename(), "file");
		String safeName = sanitizeFilename(original);
		String ext = getExt(safeName);
		if (!ALLOWED_EXT.contains(ext.toLowerCase())) {
			throw new RuntimeException("Unsupported file type: " + ext);
		}

		try {
			// Create dirs
			Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
			Path folder = root.resolve(subfolder).normalize();
			Files.createDirectories(folder);

			// Unique filename to avoid collisions
			String unique = UUID.randomUUID().toString() + "_" + safeName;
			Path dest = folder.resolve(unique).normalize();

			// Path traversal guard
			if (!dest.startsWith(folder)) {
				throw new RuntimeException("Invalid path");
			}

			// Write file
			Files.copy(aaLetterFile.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

			// Build public URL (served by StaticResourceConfig mapping /files/** ->
			// uploadRoot)
			String base = publicBase.endsWith("/") ? publicBase.substring(0, publicBase.length() - 1) : publicBase;
			return base + "/" + subfolder + "/" + unique;

		} catch (IOException e) {
			throw new RuntimeException("Failed to store file", e);
		}
	}

}