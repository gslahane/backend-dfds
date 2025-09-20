package com.lsit.dfds.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface IFileUploadService {
	String uploadFile(String filePath, String fileName, MultipartFile file) throws IOException;
}