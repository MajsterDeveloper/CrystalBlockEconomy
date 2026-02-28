package world.bentobox.crystalblockeconomy.api;

import world.bentobox.crystalblockeconomy.economy.AccountType;

import java.util.UUID;

public interface EconomyApi {

    // ===== PLAYER =====
    long getBalance(UUID playerUuid, AccountType type);

    /**
     * Dodaje środki. amount > 0.
     */
    boolean deposit(UUID playerUuid, AccountType type, long amount);

    /**
     * Zdejmuje środki. amount > 0.
     * Zwraca false jeśli brak środków.
     */
    boolean withdraw(UUID playerUuid, AccountType type, long amount);

    /**
     * Transfer między graczami z konta `type`.
     */
    boolean transfer(UUID from, UUID to, AccountType type, long amount);

    // ===== ISLAND BANK =====
    long getIslandBank(UUID islandUuid);

    boolean depositIsland(UUID islandUuid, long amount);

    boolean withdrawIsland(UUID islandUuid, long amount);
}