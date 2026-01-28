package com.darkgolly.filesharing.service;

import com.darkgolly.filesharing.model.FileRecord;
import com.darkgolly.filesharing.repository.FileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CleanupService {
    private final Path storageDir;
    private final FileRepository repository;
    private final Duration expiration;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public CleanupService(Path storageDir, FileRepository repository, Duration expiration) {
        this.storageDir = storageDir;
        this.repository = repository;
        this.expiration = expiration;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 24, TimeUnit.HOURS);
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minus(expiration);
        List<FileRecord> expired = repository.findExpired(cutoff);
        for (FileRecord record : expired) {
            try {
                Files.deleteIfExists(record.storagePath());
            } catch (IOException ignored) {
            }
            repository.remove(record.token());
        }

        try {
            Set<Path> known = new HashSet<>(repository.storagePaths());
            Files.list(storageDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> !known.contains(path))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}
