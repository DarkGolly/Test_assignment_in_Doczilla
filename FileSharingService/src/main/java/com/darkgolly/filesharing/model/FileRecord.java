package com.darkgolly.filesharing.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public record FileRecord(
        String token,
        String owner,
        String originalName,
        Path storagePath,
        long size,
        Instant uploadedAt,
        Instant lastDownloaded,
        long downloadCount,
        List<DownloadEvent> downloads
) {
}

