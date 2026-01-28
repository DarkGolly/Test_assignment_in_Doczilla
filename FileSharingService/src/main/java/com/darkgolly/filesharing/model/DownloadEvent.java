package com.darkgolly.filesharing.model;

import java.time.Instant;

public record DownloadEvent(String username, Instant downloadedAt) {
}

