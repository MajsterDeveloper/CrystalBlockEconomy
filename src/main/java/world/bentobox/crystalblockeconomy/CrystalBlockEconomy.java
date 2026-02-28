package world.bentobox.crystalblockeconomy;

import org.bukkit.Bukkit;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.crystalblockeconomy.economy.EconomyService;
import world.bentobox.crystalblockeconomy.listeners.DeathLossListener;
import world.bentobox.crystalblockeconomy.storage.Storage;
import world.bentobox.crystalblockeconomy.storage.StorageFactory;

public class CrystalBlockEconomy extends Addon {

    private Storage storage;
    private EconomyService economyService;

    @Override
    public void onLoad() {
        super.onLoad();
        log("CrystalBlockEconomy onLoad()");
    }

    @Override
    public void onEnable() {
        if (getPlugin() == null || !getPlugin().isEnabled()) {
            Bukkit.getLogger().severe("BentoBox is not available or disabled!");
            setState(State.DISABLED);
            return;
        }


        // Podpinamy instancję addona do JavaPlugin (żeby /pay działało + API)
        CrystalBlockEconomyPlugin plugin = CrystalBlockEconomyPlugin.getInstance();
        if (plugin == null) {
            logError("CrystalBlockEconomyPlugin instance is null (plugin not loaded?)");
            setState(State.DISABLED);
            return;
        }
        plugin.setAddon(this);

        // init storage
        try {
            this.storage = StorageFactory.create(this);
            this.storage.init();
        } catch (Exception ex) {
            logError("Storage init failed: " + ex.getMessage());
            ex.printStackTrace();
            setState(State.DISABLED);
            return;
        }

        // init economy + API
        this.economyService = new EconomyService(this);
        plugin.registerApi(this.economyService);

        // listeners
        registerListener(new DeathLossListener(this));

        log("CrystalBlockEconomy enabled!");
    }

    @Override
    public void onReload() {
        super.onReload();

        // Po reloadzie warto ponownie zarejestrować API (na wszelki wypadek)
        CrystalBlockEconomyPlugin plugin = CrystalBlockEconomyPlugin.getInstance();
        if (plugin != null && economyService != null) {
            plugin.registerApi(economyService);
        }

        // Jeśli kiedyś dodasz Settings/ConfigObject - tu będziesz go reloadował.
        // Storage reload (opcjonalnie): jak zmienisz storage.type na żywo, to zrób re-init
        // (na razie zostawiamy bez grzebania, bo działa i jest bezpiecznie).
        log("CrystalBlockEconomy reloaded!");
    }

    @Override
    public void onDisable() {
        // API unregister (żeby nie zostało po reloadach)
        CrystalBlockEconomyPlugin plugin = CrystalBlockEconomyPlugin.getInstance();
        if (plugin != null) {
            plugin.unregisterApi();
            plugin.setAddon(null);
        }

        if (storage != null) {
            storage.close();
            storage = null;
        }
        economyService = null;

        log("CrystalBlockEconomy disabled!");
    }

    public Storage getStorage() {
        return storage;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }
}