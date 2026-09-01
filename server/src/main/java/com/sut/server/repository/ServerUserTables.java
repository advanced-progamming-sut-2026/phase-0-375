package com.sut.server.repository;

import model.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Shared in-memory indexes for {@link ServerUserRepository}.
 */
final class ServerUserTables {

    final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(false);
    final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    final Map<String, User> usersByEmail = new ConcurrentHashMap<>();
    final List<User> users = new ArrayList<>();

    User getByUsername(String username) {
        return usersByUsername.get(username);
    }
}
