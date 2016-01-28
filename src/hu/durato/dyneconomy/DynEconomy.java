package hu.durato.dyneconomy;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Worth;

import hu.durato.dyneconomy.commands.DecayPricesCommand;
import hu.durato.dyneconomy.commands.UpdateSignCommand;

import java.io.File;
import java.util.logging.Level;

public class DynEconomy extends JavaPlugin{
	static JavaPlugin instance;
	public static Essentials essentials;
	public static Worth worth;
	FileConfiguration config;
	public static StockHandler stockHandler;
	
	public static JavaPlugin getInstance()
	{
		return instance;
	}
	
	@Override
	public void onEnable() {
		DynEconomy.instance = this;
		DynEconomy.essentials = (Essentials)Bukkit.getPluginManager().getPlugin("Essentials");
		
		if(DynEconomy.essentials==null)
		{
			Bukkit.getLogger().log(Level.SEVERE, "No Essentials");
			return;
		}
		
		DynEconomy.worth = DynEconomy.essentials.getWorth();
		
		if(DynEconomy.worth == null)
		{
			Bukkit.getLogger().log(Level.SEVERE, "No Worth");
			return;
		}
		
		File dynEconomy = new File(getDataFolder() + "config.yml");
		config = getConfig();
		
		if(!dynEconomy.exists())
	    {
	        config.addDefault("settings.buysellratio", 0.8d);
	        config.options().copyDefaults(true);
	        saveConfig();
	    }
		
		/*
		 	if(!config.contains("general.cake.eat")) {
			config.set("general.cake.eat", false);
			saveConfig();
			}
		 */
		
		// TODO: register signs in radius!
		// TODO: no/less decay without online users
		DynEconomy.stockHandler = new StockHandler(config);
		this.getCommand("updatesigns").setExecutor(new UpdateSignCommand());
		this.getCommand("decayprices").setExecutor(new DecayPricesCommand());
		getServer().getPluginManager().registerEvents(new MyListener(stockHandler, config), this);
	}
	
	@Override
	public void onDisable() {
	}
}
