package com.darkgolly.filesharing.model;

public record User(String username, String passwordHash, String salt) {
}

