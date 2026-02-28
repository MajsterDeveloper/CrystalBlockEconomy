package world.bentobox.crystalblockeconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import world.bentobox.crystalblockeconomy.CrystalBlockEconomy;
import world.bentobox.crystalblockeconomy.CrystalBlockEconomyPlugin;

public class AddMoneyCommand implements CommandExecutor {

    private final CrystalBlockEconomyPlugin plugin;

    public AddMoneyCommand(CrystalBlockEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        CrystalBlockEconomy addon = plugin.getAddon();
        if (addon == null || addon.getState() == world.bentobox.bentobox.api.addons.Addon.State.DISABLED) {
            sender.sendMessage("§cAddon ekonomii jeszcze się ładuje lub jest wyłączony.");
            return true;
        }

        if (!sender.hasPermission("crystalblockeconomy.admin.addmoney")) {
            sender.sendMessage("§cBrak uprawnień.");
            return true;
        }

        if (addon.getEconomyService() == null) {
            sender.sendMessage("§cEconomy jeszcze się ładuje. Spróbuj za chwilę.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§7Użycie: §f/addmoney <gracz_online> <kwota>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cGracz musi być online.");
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cKwota musi być liczbą całkowitą.");
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage("§cKwota musi być > 0.");
            return true;
        }

        addon.getEconomyService().depositCash(target.getUniqueId(), amount);

        sender.sendMessage("§aDodano §f" + amount + "§a dla §f" + target.getName() + "§a.");
        target.sendMessage("§aOtrzymałeś §f" + amount + "§a (admin).");
        return true;
    }
}