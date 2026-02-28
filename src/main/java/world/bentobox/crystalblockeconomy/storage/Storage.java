package world.bentobox.crystalblockeconomy.storage;

import java.util.UUID;

public interface Storage {

    void init();

    // ===== READ =====
    long getPlayerCash(UUID player);
    long getPlayerBank(UUID player);
    long getIslandBank(UUID island);

    // ===== SET =====
    void setPlayerCash(UUID player, long amount);
    void setPlayerBank(UUID player, long amount);
    void setIslandBank(UUID island, long amount);

    // ===== ADD (atomic) =====
    boolean addPlayerCash(UUID player, long delta);
    boolean addPlayerBank(UUID player, long delta);
    boolean addIslandBank(UUID island, long delta);

    // ===== TRANSFERS (atomic) =====
    boolean transferPlayerCash(UUID from, UUID to, long amount);
    boolean transferPlayerBank(UUID from, UUID to, long amount);

    void close();
}