package world.bentobox.crystalblockeconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import world.bentobox.crystalblockeconomy.CrystalBlockEconomy;
import world.bentobox.crystalblockeconomy.CrystalBlockEconomyPlugin;
import world.bentobox.crystalblockeconomy.economy.AccountType;

public class PayCommand implements CommandExecutor {

    private final CrystalBlockEconomyPlugin plugin;

    public PayCommand(CrystalBlockEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

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

        if (args.length < 2) {
            player.sendMessage("§7Użycie: §f/pay <gracz_online> <kwota>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§cGracz musi być online.");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cNie możesz przelać samemu sobie.");
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cKwota musi być liczbą całkowitą.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage("§cKwota musi być > 0.");
            return true;
        }

        boolean ok = addon.getEconomyService().transfer(
                player.getUniqueId(),
                target.getUniqueId(),
                AccountType.CASH,
                amount
        );

        if (!ok) {
            player.sendMessage("§cNie masz tyle gotówki.");
            return true;
        }

        player.sendMessage("§aPrzelano §f" + amount + "§a do §f" + target.getName() + "§a.");
        target.sendMessage("§aOtrzymałeś §f" + amount + "§a od §f" + player.getName() + "§a.");
        return true;
    }
}