package com.postofficer;

import com.postofficer.commands.POCommand;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public class PostOfficer extends JavaPlugin {

    private POBoxManager poBoxManager;
    private PostOfficeArea postOfficeArea;
    private static Economy econ = null;

    @Override
    public void onEnable() {
        // Save the default config if it doesn't exist
        saveDefaultConfig();

        // Initialize POBoxManager and PostOfficeArea
        this.poBoxManager = new POBoxManager();
        this.postOfficeArea = new PostOfficeArea(this);

        // Setup economy
        setupEconomy();

        // Register event listeners (pass the plugin instance as the third argument)
        getServer().getPluginManager().registerEvents(new EventListener(this.poBoxManager, this.postOfficeArea, this), this);

        // Register commands
        this.getCommand("po").setExecutor(new POCommand(this.poBoxManager, this.postOfficeArea));

        getLogger().info("PostOfficer Plugin Enabled");
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault plugin not found! Continuing without economy support.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("Economy provider not found! Continuing without economy support.");
            return;
        }
        econ = rsp.getProvider();
    }

    public Economy getEconomy() {
        return econ;
    }

    public String getPostOfficeAccount() {
        return "PostOffice"; // Replace with actual account name if different
    }

    @Override
    public void onDisable() {
        if (poBoxManager != null) {
            poBoxManager.saveData();
        }
        getLogger().info("PostOfficer Plugin Disabled");
    }

    // Provide access to POBoxManager for the Post Office area
    public POBoxManager getPoBoxManager() {
        return poBoxManager;
    }
}