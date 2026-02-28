package world.bentobox.crystalblockeconomy;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import world.bentobox.crystalblockeconomy.api.EconomyApi;
import world.bentobox.crystalblockeconomy.commands.AddMoneyCommand;
import world.bentobox.crystalblockeconomy.commands.MoneyCommand;
import world.bentobox.crystalblockeconomy.commands.PayCommand;

public class CrystalBlockEconomyPlugin extends JavaPlugin {

    private static CrystalBlockEconomyPlugin instance;
    private volatile CrystalBlockEconomy addon; // ustawiane przez Addon

    @Override
    public void onLoad() {
        instance = this;
    }

    public static CrystalBlockEconomyPlugin getInstance() {
        return instance;
    }

    public CrystalBlockEconomy getAddon() {
        return addon;
    }

    public void setAddon(CrystalBlockEconomy addon) {
        this.addon = addon;
    }

    public void registerApi(EconomyApi api) {
        // czyścimy poprzednią rejestrację tej usługi od tego pluginu
        Bukkit.getServicesManager().unregister(EconomyApi.class, this);

        Bukkit.getServicesManager().register(
                EconomyApi.class,
                api,
                this,
                ServicePriority.Normal
        );
    }

    public void unregisterApi() {
        Bukkit.getServicesManager().unregister(EconomyApi.class, this);
    }

    @Override
    public void onEnable() {

        // komendy (plugin.yml)
        registerCmd("pay", new PayCommand(this));
        registerCmd("money", new MoneyCommand(this));
        registerCmd("addmoney", new AddMoneyCommand(this));


    }

    @Override
    public void onDisable() {
        unregisterApi();
    }

    private void registerCmd(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("Command '" + name + "' is missing in plugin.yml");
            return;
        }
        cmd.setExecutor(executor);
    }
}