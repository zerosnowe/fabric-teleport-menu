package com.example.server.storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the single SQLite connection used by the server and applies the schema.
 */
public final class DatabaseManager implements AutoCloseable {
	private static final int SCHEMA_VERSION = 1;

	private final Connection connection;

	public DatabaseManager(Path databaseFile) throws SQLException {
		loadDriver();
		this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
		configure();
		applySchema();
	}

	private static void loadDriver() throws SQLException {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException error) {
			throw new SQLException("SQLite JDBC driver is not available on the classpath", error);
		}
	}

	private void configure() throws SQLException {
		try (Statement statement = this.connection.createStatement()) {
			statement.execute("PRAGMA journal_mode = WAL");
			statement.execute("PRAGMA synchronous = NORMAL");
			statement.execute("PRAGMA foreign_keys = ON");
			statement.execute("PRAGMA busy_timeout = 5000");
		}
	}

	private void applySchema() throws SQLException {
		try (Statement statement = this.connection.createStatement()) {
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS meta (
					key TEXT PRIMARY KEY,
					value TEXT NOT NULL
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS homes (
					owner TEXT NOT NULL,
					id TEXT NOT NULL,
					name TEXT NOT NULL,
					x INTEGER NOT NULL,
					y INTEGER NOT NULL,
					z INTEGER NOT NULL,
					icon_item_id TEXT,
					sort_order INTEGER NOT NULL DEFAULT 0,
					PRIMARY KEY (owner, id)
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS territories (
					owner TEXT NOT NULL,
					id TEXT NOT NULL,
					owner_name TEXT,
					name TEXT NOT NULL,
					dimension_id TEXT NOT NULL,
					min_x INTEGER NOT NULL,
					min_y INTEGER NOT NULL,
					min_z INTEGER NOT NULL,
					max_x INTEGER NOT NULL,
					max_y INTEGER NOT NULL,
					max_z INTEGER NOT NULL,
					icon_item_id TEXT,
					build_break_allowed INTEGER NOT NULL,
					container_allowed INTEGER NOT NULL,
					interact_allowed INTEGER NOT NULL,
					damage_entities_allowed INTEGER NOT NULL,
					block_use_allowed INTEGER NOT NULL,
					sort_order INTEGER NOT NULL DEFAULT 0,
					PRIMARY KEY (owner, id)
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS territory_whitelist (
					territory_id TEXT NOT NULL,
					player_id TEXT NOT NULL,
					PRIMARY KEY (territory_id, player_id)
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS shops (
					owner TEXT NOT NULL,
					id TEXT NOT NULL,
					name TEXT NOT NULL,
					owner_name TEXT,
					icon_item_id TEXT,
					sort_order INTEGER NOT NULL DEFAULT 0,
					PRIMARY KEY (owner, id)
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS shop_goods (
					shop_id TEXT NOT NULL,
					item_id TEXT NOT NULL,
					name TEXT,
					icon_item_id TEXT,
					stock INTEGER NOT NULL,
					price REAL NOT NULL,
					sort_order INTEGER NOT NULL DEFAULT 0,
					PRIMARY KEY (shop_id, item_id)
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS box_shops (
					id TEXT PRIMARY KEY,
					shop_id TEXT NOT NULL,
					owner_id TEXT NOT NULL,
					owner_name TEXT,
					name TEXT NOT NULL,
					dimension_id TEXT NOT NULL,
					x INTEGER NOT NULL,
					y INTEGER NOT NULL,
					z INTEGER NOT NULL,
					named INTEGER NOT NULL
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS shop_limits (
					owner TEXT PRIMARY KEY,
					limit_value INTEGER NOT NULL
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS rewarded_equipment (
					owner TEXT NOT NULL,
					item_id TEXT NOT NULL,
					PRIMARY KEY (owner, item_id)
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS moments (
					id TEXT PRIMARY KEY,
					author_id TEXT NOT NULL,
					author_name TEXT,
					content TEXT NOT NULL,
					created_at INTEGER NOT NULL
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS moment_likes (
					moment_id TEXT NOT NULL,
					player_id TEXT NOT NULL,
					PRIMARY KEY (moment_id, player_id)
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS moment_drafts (
					player_id TEXT PRIMARY KEY,
					content TEXT NOT NULL
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS disabled_items (
					item_id TEXT PRIMARY KEY
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS economy_sidebar_hidden (
					player_id TEXT PRIMARY KEY
				)
				""");
		}

		try (PreparedStatement statement = this.connection.prepareStatement(
			"INSERT OR IGNORE INTO meta(key, value) VALUES ('schema_version', ?)")) {
			statement.setString(1, Integer.toString(SCHEMA_VERSION));
			statement.executeUpdate();
		}
	}

	public Connection connection() {
		return this.connection;
	}

	public boolean hasMeta(String key) throws SQLException {
		try (PreparedStatement statement = this.connection.prepareStatement("SELECT 1 FROM meta WHERE key = ?")) {
			statement.setString(1, key);
			try (ResultSet result = statement.executeQuery()) {
				return result.next();
			}
		}
	}

	public void setMeta(String key, String value) throws SQLException {
		try (PreparedStatement statement = this.connection.prepareStatement(
			"INSERT INTO meta(key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value")) {
			statement.setString(1, key);
			statement.setString(2, value);
			statement.executeUpdate();
		}
	}

	public void transaction(SqlWork work) throws SQLException {
		boolean previousAutoCommit = this.connection.getAutoCommit();
		this.connection.setAutoCommit(false);
		try {
			work.run(this.connection);
			this.connection.commit();
		} catch (SQLException | RuntimeException error) {
			this.connection.rollback();
			throw error;
		} finally {
			this.connection.setAutoCommit(previousAutoCommit);
		}
	}

	@Override
	public void close() throws SQLException {
		this.connection.close();
	}

	@FunctionalInterface
	public interface SqlWork {
		void run(Connection connection) throws SQLException;
	}
}
