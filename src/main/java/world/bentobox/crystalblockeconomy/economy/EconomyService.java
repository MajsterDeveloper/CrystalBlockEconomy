package world.bentobox.crystalblockeconomy.economy;

import world.bentobox.crystalblockeconomy.CrystalBlockEconomy;
import world.bentobox.crystalblockeconomy.api.EconomyApi;
import world.bentobox.crystalblockeconomy.storage.Storage;

import java.util.UUID;

public class EconomyService implements EconomyApi {

    private final CrystalBlockEconomy addon;
    private final Storage storage;

    public EconomyService(CrystalBlockEconomy addon) {
        this.addon = addon;
        this.storage = addon.getStorage();
    }

    // =========================
    // PLAYER CASH (atomic via Storage)
    // =========================

    public long getCash(UUID player) {
        try {
            return Math.max(0L, storage.getPlayerCash(player));
        } catch (Exception e) {
            addon.logError("getCash failed for " + player + ": " + e.getMessage());
            return 0L;
        }
    }

    public void setCash(UUID player, long amount) {
        try {
            storage.setPlayerCash(player, Math.max(0L, amount));
        } catch (Exception e) {
            addon.logError("setCash failed for " + player + ": " + e.getMessage());
        }
    }

    public boolean depositCash(UUID player, long amount) {
        if (amount <= 0) return false;
        try {
            return storage.addPlayerCash(player, amount);
        } catch (Exception e) {
            addon.logError("depositCash failed for " + player + ": " + e.getMessage());
            return false;
        }
    }

    public boolean withdrawCash(UUID player, long amount) {
        if (amount <= 0) return false;
        try {
            return storage.addPlayerCash(player, -amount);
        } catch (Exception e) {
            addon.logError("withdrawCash failed for " + player + ": " + e.getMessage());
            return false;
        }
    }

    public boolean payCash(UUID from, UUID to, long amount) {
        if (amount <= 0) return false;
        try {
            return storage.transferPlayerCash(from, to, amount);
        } catch (Exception e) {
            addon.logError("payCash failed: " + from + " -> " + to + ": " + e.getMessage());
            return false;
        }
    }

    // =========================
    // PLAYER BANK (atomic via Storage)
    // =========================

    public long getBank(UUID player) {
        try {
            return Math.max(0L, storage.getPlayerBank(player));
        } catch (Exception e) {
            addon.logError("getBank failed for " + player + ": " + e.getMessage());
            return 0L;
        }
    }

    public void setBank(UUID player, long amount) {
        try {
            storage.setPlayerBank(player, Math.max(0L, amount));
        } catch (Exception e) {
            addon.logError("setBank failed for " + player + ": " + e.getMessage());
        }
    }

    public boolean depositBank(UUID player, long amount) {
        if (amount <= 0) return false;
        try {
            return storage.addPlayerBank(player, amount);
        } catch (Exception e) {
            addon.logError("depositBank failed for " + player + ": " + e.getMessage());
            return false;
        }
    }

    public boolean withdrawBank(UUID player, long amount) {
        if (amount <= 0) return false;
        try {
            return storage.addPlayerBank(player, -amount);
        } catch (Exception e) {
            addon.logError("withdrawBank failed for " + player + ": " + e.getMessage());
            return false;
        }
    }

    public boolean payBank(UUID from, UUID to, long amount) {
        if (amount <= 0) return false;
        try {
            return storage.transferPlayerBank(from, to, amount);
        } catch (Exception e) {
            addon.logError("payBank failed: " + from + " -> " + to + ": " + e.getMessage());
            return false;
        }
    }

    // =========================
    // ISLAND BANK (atomic via Storage)
    // =========================

    @Override
    public long getIslandBank(UUID islandUuid) {
        try {
            return Math.max(0L, storage.getIslandBank(islandUuid));
        } catch (Exception e) {
            addon.logError("getIslandBank failed for " + islandUuid + ": " + e.getMessage());
            return 0L;
        }
    }

    public void setIslandBank(UUID islandUuid, long amount) {
        try {
            storage.setIslandBank(islandUuid, Math.max(0L, amount));
        } catch (Exception e) {
            addon.logError("setIslandBank failed for " + islandUuid + ": " + e.getMessage());
        }
    }

    @Override
    public boolean depositIsland(UUID islandUuid, long amount) {
        if (amount <= 0) return false;
        try {
            return storage.addIslandBank(islandUuid, amount);
        } catch (Exception e) {
            addon.logError("depositIsland failed for " + islandUuid + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean withdrawIsland(UUID islandUuid, long amount) {
        if (amount <= 0) return false;
        try {
            return storage.addIslandBank(islandUuid, -amount);
        } catch (Exception e) {
            addon.logError("withdrawIsland failed for " + islandUuid + ": " + e.getMessage());
            return false;
        }
    }

    // =========================
    // EconomyApi (PLAYER)
    // =========================

    @Override
    public long getBalance(UUID playerUuid, AccountType type) {
        if (playerUuid == null || type == null) return 0L;
        return switch (type) {
            case CASH -> getCash(playerUuid);
            case BANK -> getBank(playerUuid);
        };
    }

    @Override
    public boolean deposit(UUID playerUuid, AccountType type, long amount) {
        if (playerUuid == null || type == null || amount <= 0) return false;
        return switch (type) {
            case CASH -> depositCash(playerUuid, amount);
            case BANK -> depositBank(playerUuid, amount);
        };
    }

    @Override
    public boolean withdraw(UUID playerUuid, AccountType type, long amount) {
        if (playerUuid == null || type == null || amount <= 0) return false;
        return switch (type) {
            case CASH -> withdrawCash(playerUuid, amount);
            case BANK -> withdrawBank(playerUuid, amount);
        };
    }

    @Override
    public boolean transfer(UUID from, UUID to, AccountType type, long amount) {
        if (from == null || to == null || type == null) return false;
        if (from.equals(to)) return false;
        if (amount <= 0) return false;

        return switch (type) {
            case CASH -> payCash(from, to, amount);
            case BANK -> payBank(from, to, amount);
        };
    }
}