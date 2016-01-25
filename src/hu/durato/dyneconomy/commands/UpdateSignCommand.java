package hu.durato.dyneconomy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import hu.durato.dyneconomy.DynEconomy;

public class UpdateSignCommand implements CommandExecutor{

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("updatesigns"))
		{	
			if(DynEconomy.stockHandler != null)
			{
				DynEconomy.stockHandler.updateSigns();
			}
			sender.sendMessage("§5[DynEco]§f Signs updated.");
			return true;
		}
		
		return false;
	}

}
