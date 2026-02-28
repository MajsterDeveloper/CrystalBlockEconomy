package world.bentobox.crystalblockeconomy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import world.bentobox.crystalblockeconomy.CrystalBlockEconomy;
import world.bentobox.crystalblockeconomy.CrystalBlockEconomyPlugin;
import world.bentobox.crystalblockeconomy.economy.AccountType;

public class MoneyCommand implements CommandExecutor {

    private final CrystalBlockEconomyPlugin plugin;

    public MoneyCommand(CrystalBlockEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        CrystalBlockEconomy addon = plugin.getAddon();
        if (addon == null || addon.getState() == world.bentobox.bentobox.api.addons.Addon.State.DISABLED) {
            sender.sendMessage("§cAddon ekonomii jeszcze się ładuje lub jest wyłączony.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTa komenda jest tylko dla graczy.");
            return true;
        }

        if (addon.getEconomyService() == null) {
            player.sendMessage("§cEconomy jeszcze się ładuje. Spróbuj za chwilę.");
            return true;
        }

        long cash = addon.getEconomyService().getCash(player.getUniqueId());
        long bank = addon.getEconomyService().getBank(player.getUniqueId());

        player.sendMessage("§bTwoje saldo:");
        player.sendMessage("§7- Gotówka: §f" + cash);
        player.sendMessage("§7- Bank: §f" + bank);
        return true;
    }
}