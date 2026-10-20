package com.bcp.workshop.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class LegacyDatabase implements UserRepository {
    private static final String DB_URL = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
    private static final Pattern BCRYPT_HASH_PATTERN = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(Path.of("data"));
            try (Connection connection = DriverManager.getConnection(DB_URL); Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS legacy_users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)");
                insertSeedUser(connection, "student", "Workshop123!", "Workshop Student", "STUDENT");
                insertSeedUser(connection, "trainer", "Trainer123!", "Workshop Trainer", "TRAINER");
                migratePlainTextPasswords(connection);
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    private void insertSeedUser(Connection connection, String username, String password, String displayName, String role) throws Exception {
        String sql = "INSERT OR IGNORE INTO legacy_users(username,password,display_name,role) VALUES (?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, BcryptUtil.bcryptHash(password));
            statement.setString(3, displayName);
            statement.setString(4, role);
            statement.executeUpdate();
        }
    }

    private void migratePlainTextPasswords(Connection connection) throws Exception {
        List<StoredPassword> storedPasswords = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT id, password FROM legacy_users")) {
            while (result.next()) {
                storedPasswords.add(new StoredPassword(result.getLong("id"), result.getString("password")));
            }
        }

        String updateSql = "UPDATE legacy_users SET password=? WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            for (StoredPassword storedPassword : storedPasswords) {
                if (isBcryptHash(storedPassword.password())) {
                    continue;
                }
                statement.setString(1, BcryptUtil.bcryptHash(storedPassword.password()));
                statement.setLong(2, storedPassword.userId());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private boolean isBcryptHash(String password) {
        return password != null && BCRYPT_HASH_PATTERN.matcher(password).matches();
    }

    public UserRecord findByCredentials(String username, String password) {
        String sql = "SELECT id, username, password, display_name, role FROM legacy_users WHERE username=?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !BcryptUtil.matches(password, result.getString("password"))) {
                    return null;
                }
                return new UserRecord(result.getLong("id"), result.getString("username"), result.getString("display_name"), result.getString("role"));
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
        return null;
    }

    private record StoredPassword(long userId, String password) {}
}
