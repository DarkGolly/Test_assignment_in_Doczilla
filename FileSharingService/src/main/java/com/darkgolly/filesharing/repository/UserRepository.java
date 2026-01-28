package com.darkgolly.filesharing.repository;

import com.darkgolly.filesharing.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class UserRepository {
    private final Database database;

    public UserRepository(Database database) {
        this.database = database;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "select username, password_hash, salt from users where username = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("salt")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot query user", e);
        }
        return Optional.empty();
    }

    public boolean create(String username, String passwordHash, String salt) {
        String sql = "insert into users (username, password_hash, salt, created_at) values (?, ?, ?, ?)";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, salt);
            statement.setLong(4, Instant.now().toEpochMilli());
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
