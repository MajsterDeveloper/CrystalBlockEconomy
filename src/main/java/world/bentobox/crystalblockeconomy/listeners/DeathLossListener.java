package world.bentobox.crystalblockeconomy.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import world.bentobox.crystalblockeconomy.CrystalBlockEconomy;
import world.bentobox.crystalblockeconomy.economy.AccountType;
import world.bentobox.crystalblockeconomy.economy.EconomyService;

import java.util.concurrent.ThreadLocalRandom;

public class DeathLossListener implements Listener {

    private final CrystalBlockEconomy addon;

    public DeathLossListener(CrystalBlockEconomy addon) {
        this.addon = addon;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!addon.getConfig().getBoolean("death-loss.enabled", true)) return;

        // (opcjonalnie) tylko w światach BentoBox:
        // if (!addon.getIWM().inWorld(e.getEntity().getWorld())) return;

        double min = addon.getConfig().getDouble("death-loss.min-percent", 0.0);
        double max = addon.getConfig().getDouble("death-loss.max-percent", 10.0);
        if (max < min) { double t = min; min = max; max = t; }

        AccountType account = AccountType.fromString(addon.getConfig().getString("death-loss.account", "CASH"));
        boolean dropToGround = addon.getConfig().getBoolean("death-loss.drop-to-ground", false);

        EconomyService eco = addon.getEconomyService();

        var player = e.getEntity();
        long bal = eco.getBalance(player.getUniqueId(), account);
        if (bal <= 0) return;

        double percent = (max == min)
                ? min
                : ThreadLocalRandom.current().nextDouble(min, max);

        long loss = (long) Math.floor(bal * (percent / 100.0));
        if (loss <= 0) return;

        eco.withdraw(player.getUniqueId(), account, loss);

        // na razie tylko kasujemy; "drop-to-ground" na przyszłość
        if (dropToGround) {
            // TODO: worek / item / hologram itp.
        }

        player.sendMessage("§cStraciłeś §e" + loss + "§c (" + String.format("%.2f", percent) + "%) z konta §e" + account + "§c.");
    }
}