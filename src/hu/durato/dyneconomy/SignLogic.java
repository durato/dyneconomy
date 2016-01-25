package hu.durato.dyneconomy;

import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.block.SignChangeEvent;

public class SignLogic {
	
	public static boolean isDynEcoSign(Sign sign)
	{
		return sign.getLine(0).equals("[DynEco]") || sign.getLine(0).equals("§5[DynEco]");
	}
	
	public static boolean isDynEcoSign(SignChangeEvent event)
	{
		return event.getLine(0).equals("[DynEco]") || event.getLine(0).equals("§5[DynEco]");
	}

	public static void SaveSign(SignChangeEvent event) {
		FileConfiguration config = DynEconomy.getInstance().getConfig();
		Location loc = event.getBlock().getLocation();
		
		String idStr = loc.getWorld().getName()+".sign.pos."+loc.getBlockX()+"/"+loc.getBlockY()+"/"+loc.getBlockZ();
		
		config.set("signdata."+idStr+".placedby",event.getPlayer().getName());
		config.set("signdata."+idStr+".items",event.getLine(1));
		DynEconomy.getInstance().saveConfig();
	}
}
