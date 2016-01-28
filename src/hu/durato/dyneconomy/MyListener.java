package hu.durato.dyneconomy;

import java.math.BigDecimal;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.Worth;

import net.ess3.api.MaxMoneyException;

public class MyListener implements Listener {
	StockHandler stockHandler;
	FileConfiguration config;
	
	public MyListener(StockHandler stockHandler, FileConfiguration conf) {
		this.stockHandler = stockHandler;
		this.config = conf;
	}
	
	@EventHandler
	public void onSignChanged(SignChangeEvent event)
	{
		if(event.isCancelled() || this.stockHandler == null)
		{
			return;
		}
		
		if(!SignLogic.isDynEcoSign(event))
		{
			return;
		}
		
		if(!event.getLine(1).equals(""))
		{
			Material mat = null;
			String[] lineOneSplit = event.getLine(1).split(" ");
			
			if(lineOneSplit.length==0)
			{
				return;
			}
			
			//mat = Material.matchMaterial(lineOneSplit[lineOneSplit.length-1]); //TODO: redstone comparator? daylight_detector? steak?
			try {
				mat = DynEconomy.essentials.getItemDb().get(lineOneSplit[lineOneSplit.length-1]).getType();
				event.getPlayer().sendMessage(mat.name());
			} catch (Exception e) {
				Bukkit.getLogger().log(Level.SEVERE, "[DynEco] getItemDb failed");
				e.printStackTrace();
			}
			
			int amount = 1;
			
			if(lineOneSplit.length>1 && mat.getMaxStackSize()>1)
			{
				int newAmount = Integer.parseInt(lineOneSplit[0]);
				
				if(newAmount>0)
				{
					amount = newAmount;
				}
			}
			
			if(mat==null)
			{
				event.getPlayer().sendMessage("[DynEco] Couldn't resolve material!");
				event.setLine(0, "§4[DynEco]");
				event.setLine(1, "§4item?");
				return;
			}
			
			ItemStack oneItem = new ItemStack(mat,1);
			
			Worth worth = DynEconomy.essentials.getWorth();
			
			if(worth==null)
			{
				Bukkit.getLogger().log(Level.SEVERE, "[DynEco] getting Worth failed");
				return;
			}
			
			double buySellRatio = this.config.getDouble("settings.buysellratio");
			
			BigDecimal itemWorth = worth.getPrice(oneItem);
			
			if(itemWorth == null)
			{
				Bukkit.getLogger().log(Level.SEVERE, "[DynEco] getting item's worth failed");
				return;
			}
			
			double itemWdouble = itemWorth.doubleValue();
			//Bukkit.broadcastMessage( "[DynEco] Material is: " + mat.name() );
			
			event.setLine(0, "§5[DynEco]");
			event.setLine(2, "BUY       SELL");
			//TODO : prices' text can overflow
			event.setLine(3, Math.round(amount * itemWdouble*100.0)/100.0 + "      " + Math.round(amount * itemWdouble * buySellRatio*100.0)/100.0 );
			SignLogic.SaveSign(event);
			event.getPlayer().sendMessage( "§5[DynEco]§f Sign placed and registered.");
		}
		else
		{
			event.setLine(0, "§4[DynEco]");
			event.setLine(1, "§4ITEM?");
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if(event.isCancelled() || this.stockHandler == null)
		{
			return;
		}
		
		if(event.getClickedBlock().getType() == Material.SIGN || event.getClickedBlock().getType() == Material.WALL_SIGN) //TODO: sign_post?
		{
			Sign sign = (Sign) event.getClickedBlock().getState();
			
			if(sign==null || !SignLogic.isDynEcoSign(sign))
			{
				return;
			}
			
			double buyPrice = 0;
			
			if(sign.getLine(1).equals(""))
			{
				return;
			}
			Material mat = null;
			String[] lineOneSplit = sign.getLine(1).split(" ");
			
			if(lineOneSplit.length==0)
			{
				return;
			}
			
			Essentials ess = (Essentials)Bukkit.getPluginManager().getPlugin("Essentials");
			//mat = Material.matchMaterial(lineOneSplit[lineOneSplit.length-1]);
			try {
				mat = ess.getItemDb().get(lineOneSplit[lineOneSplit.length-1]).getType();
				//event.getPlayer().sendMessage(mat.name());
			} catch (Exception e) {
				Bukkit.getLogger().log(Level.SEVERE, "[DynEco] getItemDb failed");
				e.printStackTrace();
			}
			int amount = 1;
			
			if(lineOneSplit.length>1 && mat.getMaxStackSize()>1)
			{
				int newAmount = Integer.parseInt(lineOneSplit[0]);
				
				if(newAmount>0)
				{
					amount = newAmount;
				}
			}
			
			if(mat==null)
			{
				event.getPlayer().sendMessage("§5[DynEco]§4 Couldn't resolve material!");					
			}
			else
			{				
				if(ess == null)
				{
					Bukkit.getLogger().log(Level.SEVERE, "[DynEco] Essentials not found");
					return;
				}
							
				String[] lineThreeSplit = sign.getLine(3).split(" ");
				
				if(lineThreeSplit.length<2)
				{
					return;
				}
				
				buyPrice = Double.parseDouble(lineThreeSplit[0]);
				
				if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) 
				{
		            User user = ess.getUser(event.getPlayer());
		            PlayerInventory pi = event.getPlayer().getInventory();
		            double sellPrice = Double.parseDouble(lineThreeSplit[lineThreeSplit.length-1]);
		            
		            if(event.getPlayer().getInventory().contains(mat,amount))
		            {
		            	try {
		            		int remaining = amount;
		            		
		            		for(int i = 0; i<pi.getSize(); i++)
		            		{
		            			if(i>=pi.getSize())
		            			{
		            				break;
		            			}
		            			
		            			ItemStack is = pi.getItem(i);
		            			if(is != null && is.getType().equals(mat))
		            			{
		            				if(is.getAmount()>remaining)
		            				{
		            					is.setAmount(is.getAmount()-remaining);
		            					remaining=0;
		            					break;
		            				}
		            				else
		            				{
		            					remaining-=is.getAmount();
		            					pi.setItem(i, null);
		            				}
		            			}
		            		}
		            		
		            		event.getPlayer().updateInventory();
		            		this.stockHandler.depositStock(mat, (amount-remaining));
		            		user.giveMoney(new BigDecimal(sellPrice*(amount-remaining)/amount));
						} catch (MaxMoneyException e) {
							e.printStackTrace();
						}
		            }
		            else
		            {
		            	user.sendMessage("§5[DynEco]§3 You don't have enough of that item: " + amount +" x "+ mat);
		            }
		            
		        }
				else if(event.getAction().equals(Action.LEFT_CLICK_BLOCK))
				{
					//Bukkit.broadcastMessage( "[DynEco] Tried to buy for " + totalBuyValue );
					User user = ess.getUser(event.getPlayer());
		            BigDecimal bal = user.getMoney();
		            
		            if(bal.doubleValue()>=buyPrice)
		            {
		            	if(this.stockHandler.getStock(mat)<amount)
		            	{
		            		user.sendMessage("§5[DynEco]§3 We don't have this much of that item. Consider selling it?");
		            	}
		            	else
		            	{			            		
		            		int freeSpace = 0;
		            		
		            		for (ItemStack i : event.getPlayer().getInventory()) 
		            		{
			            		if (i == null || i.getAmount()==0) 
			            		{
			            			freeSpace+=mat.getMaxStackSize();
			            		} 
			            		else if (i.getType() == mat) 
			            		{
			            			freeSpace+=i.getType().getMaxStackSize() - i.getAmount();
			            		}
		            		}
		            		
		            		if (amount <= freeSpace) 
		            		{
		            			user.takeMoney(new BigDecimal(buyPrice));
								this.stockHandler.withdrawStock(mat, amount);
								event.getPlayer().getInventory().addItem((new ItemStack(mat, amount)));
								event.getPlayer().updateInventory();
		            		} 
		            		else
		            		{
		            			event.getPlayer().sendMessage("§5[DynEco]§3 You haven't got enough space left in your inventory!");
		            		}
		            	}
		            }
		            else
		            {
		            	user.sendMessage( "§5[DynEco]§3 You don't have enough money!" );
		            }
				}
			}			
			
		}
		
	}
}
