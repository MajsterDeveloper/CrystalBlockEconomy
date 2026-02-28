package world.bentobox.crystalblockeconomy.storage.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import world.bentobox.crystalblockeconomy.CrystalBlockEconomy;
import world.bentobox.crystalblockeconomy.CrystalBlockEconomyPlugin;
import world.bentobox.crystalblockeconomy.storage.Storage;

import java.sql.*;
import java.util.UUID;

public class MySqlStorage implements Storage {

    private final CrystalBlockEconomy addon;
    private HikariDataSource ds;

    public MySqlStorage(CrystalBlockEconomy addon) {
        this.addon = addon;
    }

    private FileConfiguration cfg() {
        return CrystalBlockEconomyPlugin.getInstance().getConfig();
    }

    @Override
    public void init() {
        String host = cfg().getString("storage.mysql.host", "localhost");
        int port = cfg().getInt("storage.mysql.port", 3306);
        String db = cfg().getString("storage.mysql.database", "crystalblock");
        String user = cfg().getString("storage.mysql.username", "root");
        String pass = cfg().getString("storage.mysql.password", "");
        boolean ssl = cfg().getBoolean("storage.mysql.useSSL", false);
        String params = cfg().getString("storage.mysql.parameters", "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + db +
                "?useSSL=" + ssl + (params == null || params.isBlank() ? "" : "&" + params);

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setPoolName("CrystalBlockEconomy-MySQL");

        // pool
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(10_000);
        cfg.setValidationTimeout(5_000);
        cfg.setLeakDetectionThreshold(0); // ewentualnie 20_000 na debug

        // perf props
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        cfg.addDataSourceProperty("useServerPrepStmts", "true");
        cfg.addDataSourceProperty("useUnicode", "true");
        cfg.addDataSourceProperty("characterEncoding", "utf8");
        cfg.addDataSourceProperty("connectionCollation", "utf8_general_ci");

        this.ds = new HikariDataSource(cfg);

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS cbe_player_accounts (
                  player_uuid VARCHAR(36) PRIMARY KEY,
                  cash BIGINT NOT NULL DEFAULT 0,
                  bank BIGINT NOT NULL DEFAULT 0
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS cbe_island_accounts (
                  island_uuid VARCHAR(36) PRIMARY KEY,
                  bank BIGINT NOT NULL DEFAULT 0
                )
            """);
        } catch (SQLException e) {
            addon.logError("MySQL init failed: " + e.getMessage());
            throw new RuntimeException(e);
        }

        addon.log("MySQL ready: " + host + ":" + port + "/" + db);
    }

    // ---------- READ ----------
    @Override public long getPlayerCash(UUID player) { ensurePlayerRow(player); return queryPlayerLong(player, "cash"); }
    @Override public long getPlayerBank(UUID player) { ensurePlayerRow(player); return queryPlayerLong(player, "bank"); }
    @Override public long getIslandBank(UUID island) { ensureIslandRow(island); return queryIslandLong(island); }

    // ---------- SET ----------
    @Override public void setPlayerCash(UUID player, long amount) { ensurePlayerRow(player); setPlayerColumn(player, "cash", clamp0(amount)); }
    @Override public void setPlayerBank(UUID player, long amount) { ensurePlayerRow(player); setPlayerColumn(player, "bank", clamp0(amount)); }
    @Override public void setIslandBank(UUID island, long amount)  { ensureIslandRow(island); setIsland(amount, island); }

    // ---------- ADD (atomic) ----------
    @Override public boolean addPlayerCash(UUID player, long delta) { return addPlayerDelta(player, "cash", delta); }
    @Override public boolean addPlayerBank(UUID player, long delta) { return addPlayerDelta(player, "bank", delta); }
    @Override public boolean addIslandBank(UUID island, long delta) { return addIslandDelta(island, delta); }

    // ---------- TRANSFER (atomic) ----------
    @Override public boolean transferPlayerCash(UUID from, UUID to, long amount) { return transferPlayerToPlayer(from, to, "cash", amount); }
    @Override public boolean transferPlayerBank(UUID from, UUID to, long amount) { return transferPlayerToPlayer(from, to, "bank", amount); }

    // ===== internals =====

    private long clamp0(long v) { return Math.max(0L, v); }

    private void ensurePlayerRow(UUID player) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 INSERT INTO cbe_player_accounts(player_uuid, cash, bank)
                 VALUES(?,0,0)
                 ON DUPLICATE KEY UPDATE player_uuid = player_uuid
             """)) {
            ps.setString(1, player.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            addon.logError("DB ensure player row failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void ensureIslandRow(UUID island) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 INSERT INTO cbe_island_accounts(island_uuid, bank)
                 VALUES(?,0)
                 ON DUPLICATE KEY UPDATE island_uuid = island_uuid
             """)) {
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
            ps.setLong(1, clamp0(amount));
            ps.setString(2, island.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            addon.logError("DB set island failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean addPlayerDelta(UUID player, String col, long delta) {
        ensurePlayerRow(player);
        String sql = "UPDATE cbe_player_accounts SET " + col + " = GREATEST(" + col + " + ?, 0) WHERE player_uuid = ?";
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

    private boolean addIslandDelta(UUID island, long delta) {
        ensureIslandRow(island);
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE cbe_island_accounts SET bank = GREATEST(bank + ?, 0) WHERE island_uuid = ?")) {
            ps.setLong(1, delta);
            ps.setString(2, island.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            addon.logError("DB add island failed: " + e.getMessage());
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