package world.bentobox.crystalblockeconomy.storage;

import org.bukkit.configuration.file.FileConfiguration;
import world.bentobox.crystalblockeconomy.CrystalBlockEconomy;
import world.bentobox.crystalblockeconomy.CrystalBlockEconomyPlugin;
import world.bentobox.crystalblockeconomy.storage.impl.MySqlStorage;
import world.bentobox.crystalblockeconomy.storage.impl.SqliteStorage;

public final class StorageFactory {

    private StorageFactory() {}

    private static FileConfiguration cfg() {
        CrystalBlockEconomyPlugin plugin = CrystalBlockEconomyPlugin.getInstance();
        if (plugin == null) {
            throw new IllegalStateException("CrystalBlockEconomyPlugin instance is null (plugin not loaded?)");
        }
        FileConfiguration c = plugin.getConfig();
        if (c == null) {
            throw new IllegalStateException("CrystalBlockEconomyPlugin.getConfig() is null (config not loaded?)");
        }
        return c;
    }

    public static Storage create(CrystalBlockEconomy addon) {
        String rawType = cfg().getString("storage.type", "sqlite");
        String type = rawType == null ? "sqlite" : rawType.trim().toLowerCase();

        addon.log("Selected storage type: " + type);

        return switch (type) {
            case "mysql" -> {
                validateMysqlConfig(addon);
                yield new MySqlStorage(addon);
            }
            case "sqlite" -> new SqliteStorage(addon);
            default -> {
                addon.logError("Unknown storage.type: " + type + " — falling back to SQLite.");
                yield new SqliteStorage(addon);
            }
        };
    }

    private static void validateMysqlConfig(CrystalBlockEconomy addon) {
        String host = cfg().getString("storage.mysql.host");
        String database = cfg().getString("storage.mysql.database");
        String username = cfg().getString("storage.mysql.username");

        if (isBlank(host) || isBlank(database) || isBlank(username)) {
            throw new IllegalStateException("""
                MySQL storage selected but configuration is incomplete!
                Required fields:
                  storage.mysql.host
                  storage.mysql.database
                  storage.mysql.username
            """);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}