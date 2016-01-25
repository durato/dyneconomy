package hu.durato.dyneconomy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import hu.durato.dyneconomy.DynEconomy;

public class DecayPricesCommand implements CommandExecutor{

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("decayprices"))
		{	
			if(DynEconomy.stockHandler != null)
			{
				DynEconomy.stockHandler.forceDecay();
			}
			
			sender.sendMessage("§5[DynEco]§f Price decay forced.");
			return true;
		}
		
		return false;
	}

}
