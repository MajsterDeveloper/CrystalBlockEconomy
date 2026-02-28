package world.bentobox.crystalblockeconomy.storage.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import world.bentobox.crystalblockeconomy.CrystalBlockEconomy;
import world.bentobox.crystalblockeconomy.CrystalBlockEconomyPlugin;
import world.bentobox.crystalblockeconomy.storage.Storage;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class SqliteStorage implements Storage {

    private final CrystalBlockEconomy addon;
    private HikariDataSource ds;

    public SqliteStorage(CrystalBlockEconomy addon) {
        this.addon = addon;
    }

    private FileConfiguration cfg() {
        return CrystalBlockEconomyPlugin.getInstance().getConfig();
    }

    @Override
    public void init() {
        String fileName = cfg().getString("storage.sqlite.file", "economy.db");
        File dbFile = new File(addon.getDataFolder(), fileName);
        File parent = dbFile.getParentFile();
        if (parent != null) parent.mkdirs();

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        cfg.setPoolName("CrystalBlockEconomy-SQLite");
        cfg.setMaximumPoolSize(4);
        cfg.setConnectionTestQuery("SELECT 1");

        this.ds = new HikariDataSource(cfg);

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS cbe_player_accounts (
                  player_uuid TEXT PRIMARY KEY,
                  cash       INTEGER NOT NULL DEFAULT 0,
                  bank       INTEGER NOT NULL DEFAULT 0
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS cbe_island_accounts (
                  island_uuid TEXT PRIMARY KEY,
                  bank        INTEGER NOT NULL DEFAULT 0
                )
            """);
        } catch (SQLException e) {
            addon.logError("SQLite init failed: " + e.getMessage());
            throw new RuntimeException(e);
        }

        addon.log("SQLite ready: " + dbFile.getName());
    }

    // ---------- READ ----------
    @Override public long getPlayerCash(UUID player) { ensurePlayerRow(player); return queryPlayerLong(player, "cash"); }
    @Override public long getPlayerBank(UUID player) { ensurePlayerRow(player); return queryPlayerLong(player, "bank"); }
    @Override public long getIslandBank(UUID islandUuid) { ensureIslandRow(islandUuid); return queryIslandLong(islandUuid); }

    // ---------- SET ----------
    @Override public void setPlayerCash(UUID player, long amount) { ensurePlayerRow(player); setPlayerColumn(player, "cash", clamp0(amount)); }
    @Override public void setPlayerBank(UUID player, long amount) { ensurePlayerRow(player); setPlayerColumn(player, "bank", clamp0(amount)); }
    @Override public void setIslandBank(UUID islandUuid, long amount) { ensureIslandRow(islandUuid); setIsland(clamp0(amount), islandUuid); }

    // ---------- ADD ----------
    @Override public boolean addPlayerCash(UUID uuid, long delta) { return addPlayerDelta(uuid, "cash", delta); }
    @Override public boolean addPlayerBank(UUID uuid, long delta) { return addPlayerDelta(uuid, "bank", delta); }

    @Override
    public boolean addIslandBank(UUID island, long delta) {
        ensureIslandRow(island);
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE cbe_island_accounts SET bank = MAX(bank + ?, 0) WHERE island_uuid = ?"
             )) {
            ps.setLong(1, delta);
            ps.setString(2, island.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            addon.logError("DB add island failed: " + e.getMessage());
            return false;
        }
    }

    // ---------- TRANSFERS ----------
    @Override public boolean transferPlayerCash(UUID from, UUID to, long amount) { return transferPlayerToPlayer(from, to, "cash", amount); }
    @Override public boolean transferPlayerBank(UUID from, UUID to, long amount) { return transferPlayerToPlayer(from, to, "bank", amount); }

    // ===== internals =====

    private long clamp0(long v) { return Math.max(0L, v); }

    private void ensurePlayerRow(UUID player) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT OR IGNORE INTO cbe_player_accounts(player_uuid, cash, bank) VALUES(?,0,0)"
             )) {
            ps.setString(1, player.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            addon.logError("DB ensure player row failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void ensureIslandRow(UUID island) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT OR IGNORE INTO cbe_island_accounts(island_uuid, bank) VALUES(?,0)"
             )) {
            ps.setString(1, island.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            addon.logError("DB ensure island row failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private long queryPlayerLong(UUID player, String col) {
        String sql = "SELECT " + col + " FROM cbe_player_accounts WHERE player_uuid = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            addon.logError("DB query failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private long queryIslandLong(UUID island) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT bank FROM cbe_island_accounts WHERE island_uuid = ?")) {
            ps.setString(1, island.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            addon.logError("DB query failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void setPlayerColumn(UUID player, String col, long amount) {
        String sql = "UPDATE cbe_player_accounts SET " + col + " = ? WHERE player_uuid = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, amount);
            ps.setString(2, player.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            addon.logError("DB set failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void setIsland(long amount, UUID island) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE cbe_island_accounts SET bank = ? WHERE island_uuid = ?")) {
            ps.setLong(1, amount);
            ps.setString(2, island.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            addon.logError("DB set island failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean addPlayerDelta(UUID player, String col, long delta) {
        ensurePlayerRow(player);
        String sql = "UPDATE cbe_player_accounts SET " + col + " = MAX(" + col + " + ?, 0) WHERE player_uuid = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, delta);
            ps.setString(2, player.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            addon.logError("DB add failed: " + e.getMessage());
            return false;
        }
    }

    private boolean transferPlayerToPlayer(UUID from, UUID to, String col, long amount) {
        if (amount <= 0) return false;

        ensurePlayerRow(from);
        ensurePlayerRow(to);

        String takeSql = "UPDATE cbe_player_accounts SET " + col + " = " + col + " - ? WHERE player_uuid = ? AND " + col + " >= ?";
        String giveSql = "UPDATE cbe_player_accounts SET " + col + " = " + col + " + ? WHERE player_uuid = ?";

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);

            // IMMEDIATE -> blokuje zapis, żeby dwa transfery nie zjadały się naraz
            try (Statement st = c.createStatement()) {
                st.execute("BEGIN IMMEDIATE");
            }

            try (PreparedStatement take = c.prepareStatement(takeSql);
                 PreparedStatement give = c.prepareStatement(giveSql)) {

                take.setLong(1, amount);
                take.setString(2, from.toString());
                take.setLong(3, amount);

                int took = take.executeUpdate();
                if (took <= 0) {
                    c.rollback();
                    return false;
                }

                give.setLong(1, amount);
                give.setString(2, to.toString());
                give.executeUpdate();

                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                addon.logError("DB transfer failed: " + e.getMessage());
                return false;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            addon.logError("DB transfer connection failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        if (ds != null) {
            ds.close();
            ds = null;
        }
    }

}