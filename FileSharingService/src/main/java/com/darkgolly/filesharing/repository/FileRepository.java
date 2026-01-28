package com.darkgolly.filesharing.repository;

import com.darkgolly.filesharing.model.DownloadEvent;
import com.darkgolly.filesharing.model.FileRecord;
import com.darkgolly.filesharing.util.Json;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class FileRepository {
    private final Database database;

    public FileRepository(Database database) {
        this.database = database;
    }

    public void put(FileRecord record) {
        String sql = """
                insert into files (token, owner, original_name, storage_path, size, uploaded_at, last_downloaded, download_count)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.token());
            statement.setString(2, record.owner());
            statement.setString(3, record.originalName());
            statement.setString(4, record.storagePath().toString());
            statement.setLong(5, record.size());
            statement.setLong(6, record.uploadedAt().toEpochMilli());
            statement.setLong(7, record.lastDownloaded().toEpochMilli());
            statement.setLong(8, record.downloadCount());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot insert file record", e);
        }
    }

    public Optional<FileRecord> find(String token) {
        String sql = "select * from files where token = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapFile(rs, List.of()));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot query file", e);
        }
        return Optional.empty();
    }

    public void remove(String token) {
        try (Connection connection = database.connect()) {
            try (PreparedStatement deleteDownloads = connection.prepareStatement("delete from downloads where file_token = ?")) {
                deleteDownloads.setString(1, token);
                deleteDownloads.executeUpdate();
            }
            try (PreparedStatement deleteFile = connection.prepareStatement("delete from files where token = ?")) {
                deleteFile.setString(1, token);
                deleteFile.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot удалить файл", e);
        }
    }

    public List<FileRecord> findByOwner(String owner) {
        List<FileRecord> result = new ArrayList<>();
        String sql = "select * from files where owner = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, owner);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String token = rs.getString("token");
                    List<DownloadEvent> downloads = loadDownloads(connection, token);
                    result.add(mapFile(rs, downloads));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot query files by owner", e);
        }
        return result;
    }

    public List<FileRecord> findExpired(Instant cutoff) {
        List<FileRecord> result = new ArrayList<>();
        String sql = "select * from files where last_downloaded < ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cutoff.toEpochMilli());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(mapFile(rs, List.of()));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot query expired files", e);
        }
        return result;
    }

    public List<Path> storagePaths() {
        List<Path> result = new ArrayList<>();
        String sql = "select storage_path from files";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(Path.of(rs.getString("storage_path")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot query storage paths", e);
        }
        return result;
    }

    public void updateDownload(String token, Instant downloadedAt, String username) {
        try (Connection connection = database.connect()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "insert into downloads (file_token, username, downloaded_at) values (?, ?, ?)")) {
                insert.setString(1, token);
                insert.setString(2, username);
                insert.setLong(3, downloadedAt.toEpochMilli());
                insert.executeUpdate();
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "update files set last_downloaded = ?, download_count = download_count + 1 where token = ?")) {
                update.setLong(1, downloadedAt.toEpochMilli());
                update.setString(2, token);
                update.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot update download", e);
        }
    }

    public String toJsonForOwner(String owner) {
        List<FileRecord> list = findByOwner(owner);
        list.sort(Comparator.comparing(FileRecord::uploadedAt).reversed());
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        builder.append("\"total\":").append(list.size()).append(',');
        builder.append("\"files\":[");
        for (int i = 0; i < list.size(); i++) {
            FileRecord record = list.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append('{')
                    .append("\"token\":\"").append(Json.escape(record.token())).append("\",")
                    .append("\"name\":\"").append(Json.escape(record.originalName())).append("\",")
                    .append("\"size\":").append(record.size()).append(',')
                    .append("\"uploadedAt\":\"").append(record.uploadedAt()).append("\",")
                    .append("\"lastDownloaded\":\"").append(record.lastDownloaded()).append("\",")
                    .append("\"downloadCount\":").append(record.downloadCount()).append(',')
                    .append("\"downloads\":[");
            List<DownloadEvent> events = record.downloads();
            for (int j = 0; j < events.size(); j++) {
                DownloadEvent event = events.get(j);
                if (j > 0) {
                    builder.append(',');
                }
                builder.append('{')
                        .append("\"user\":\"").append(Json.escape(event.username())).append("\",")
                        .append("\"downloadedAt\":\"").append(event.downloadedAt()).append("\"")
                        .append('}');
            }
            builder.append(']');
            builder.append('}');
        }
        builder.append(']');
        builder.append('}');
        return builder.toString();
    }

    private List<DownloadEvent> loadDownloads(Connection connection, String token) throws SQLException {
        List<DownloadEvent> events = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "select username, downloaded_at from downloads where file_token = ? order by downloaded_at desc")) {
            statement.setString(1, token);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    events.add(new DownloadEvent(
                            rs.getString("username"),
                            Instant.ofEpochMilli(rs.getLong("downloaded_at"))
                    ));
                }
            }
        }
        return events;
    }

    private FileRecord mapFile(ResultSet rs, List<DownloadEvent> downloads) throws SQLException {
        return new FileRecord(
                rs.getString("token"),
                rs.getString("owner"),
                rs.getString("original_name"),
                Path.of(rs.getString("storage_path")),
                rs.getLong("size"),
                Instant.ofEpochMilli(rs.getLong("uploaded_at")),
                Instant.ofEpochMilli(rs.getLong("last_downloaded")),
                rs.getLong("download_count"),
                downloads
        );
    }
}
