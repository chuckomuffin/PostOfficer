package com.postofficer;

import com.postofficer.commands.POCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class PostOfficer extends JavaPlugin {

    private POBoxManager poBoxManager;
    private PostOfficeArea postOfficeArea;

    @Override
    public void onEnable() {
        // Initialize POBoxManager and PostOfficeArea
        this.poBoxManager = new POBoxManager();
        this.postOfficeArea = new PostOfficeArea(this);

        // Register event listeners (pass the plugin instance as the third argument)
        getServer().getPluginManager().registerEvents(new EventListener(this.poBoxManager, this.postOfficeArea, this), this);

        // Register commands
        this.getCommand("po").setExecutor(new POCommand(this.poBoxManager, this.postOfficeArea));

        getLogger().info("PostOfficer Plugin Enabled");
    }

    @Override
    public void onDisable() {
        // Save data if necessary
        poBoxManager.saveData();
        getLogger().info("PostOfficer Plugin Disabled");
    }

    // Provide access to POBoxManager for the Post Office area
    public POBoxManager getPoBoxManager() {
        return poBoxManager;
    }
}
