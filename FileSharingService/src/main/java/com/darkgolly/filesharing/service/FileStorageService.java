package com.darkgolly.filesharing.service;

import com.darkgolly.filesharing.model.FileRecord;
import com.darkgolly.filesharing.repository.FileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FileStorageService {
    private final Path storageDir;
    private final FileRepository repository;

    public FileStorageService(Path storageDir, FileRepository repository) {
        this.storageDir = storageDir;
        this.repository = repository;
    }

    public FileRecord store(String owner, String originalName, byte[] data) throws IOException {
        String token = UUID.randomUUID().toString().replace("-", "");
        String safeName = sanitizeName(originalName);
        Path path = storageDir.resolve(token + "_" + safeName);
        Files.write(path, data);
        Instant now = Instant.now();
        FileRecord record = new FileRecord(token, owner, originalName, path, data.length, now, now, 0, List.of());
        repository.put(record);
        return record;
    }

    public Optional<FileRecord> find(String token) {
        return repository.find(token);
    }

    public void markDownloaded(String token, Instant at, String username) {
        repository.updateDownload(token, at, username);
    }

    private String sanitizeName(String name) {
        String trimmed = name == null ? "file" : name.trim();
        if (trimmed.isEmpty()) {
            return "file";
        }
        return trimmed.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}


