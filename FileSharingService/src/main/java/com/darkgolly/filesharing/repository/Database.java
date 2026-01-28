package com.darkgolly.filesharing.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final String url;

    public Database(String url) {
        this.url = url;
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public void init() {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    create table if not exists users (
                        id identity primary key,
                        username varchar(255) not null unique,
                        password_hash varchar(255) not null,
                        salt varchar(255) not null,
                        created_at bigint not null
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists files (
                        id identity primary key,
                        token varchar(64) not null unique,
                        owner varchar(255) not null,
                        original_name varchar(512) not null,
                        storage_path varchar(1024) not null,
                        size bigint not null,
                        uploaded_at bigint not null,
                        last_downloaded bigint not null,
                        download_count bigint not null
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists downloads (
                        id identity primary key,
                        file_token varchar(64) not null,
                        username varchar(255) not null,
                        downloaded_at bigint not null
                    )
                    """);
            statement.executeUpdate("create index if not exists idx_files_owner on files(owner)");
            statement.executeUpdate("create index if not exists idx_files_last_downloaded on files(last_downloaded)");
            statement.executeUpdate("create index if not exists idx_downloads_token on downloads(file_token)");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot initialize database", e);
        }
    }
}
