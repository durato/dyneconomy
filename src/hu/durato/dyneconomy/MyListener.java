package hu.durato.dyneconomy;

import java.math.BigDecimal;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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

	private void respond(Player player, String msg) {
		player.sendMessage("§5[DynEco] §6" + msg);
	}

	@EventHandler
	public void onSignChanged(SignChangeEvent event) {
		if (event.isCancelled() || this.stockHandler == null) {
			return;
		}

		if (!SignLogic.isDynEcoSign(event) || !event.getPlayer().hasPermission("dyneconomy.sign.create")) {
			// event.getPlayer().sendMessage("§5[DynEco] §6Event dropped.");
			return;
		}

		if (event.getLine(1).equals("")) {
			event.setLine(0, "§4[DynEco]");
			event.setLine(1, "§4ITEM?");
			return;
		}

		String[] lineOneSplit = event.getLine(1).split(" ");

		if (lineOneSplit.length == 0) {
			event.setLine(1, "§4item?");
			return;
		}

		int amount = 1;
		ItemStack oneItem = null;

		try {
			if (lineOneSplit.length > 1) {
				int newAmount = Integer.parseInt(lineOneSplit[0]);

				if (newAmount > 0) {
					amount = newAmount;
				}

				oneItem = DynEconomy.essentials.getItemDb().get(lineOneSplit[1]);
			} else {
				oneItem = DynEconomy.essentials.getItemDb().get(lineOneSplit[0]);
			}
		} catch (Exception ex) {
			this.respond(event.getPlayer(), "Please check item format!");
			event.setLine(0, "§4[DynEco]");
			event.setLine(1, "§4item?");
			return;
		}

		// this.respond(event.getPlayer(), oneItem.toString());
		Worth worth = DynEconomy.worth;

		double buySellRatio = this.config.getDouble("settings.buysellratio");

		BigDecimal itemWorth = worth.getPrice(oneItem);

		if (itemWorth == null) {
			Bukkit.getLogger().log(Level.SEVERE, "[DynEco] getting item's worth failed");
			event.setLine(0, "§4[DynEco]");
			event.setLine(1, "§4Can you sell");
			event.setLine(2, "§4this item?");
			return;
		}

		double itemWdouble = itemWorth.doubleValue();

		event.setLine(0, "§5[DynEco]");
		event.setLine(2, "BUY       SELL");
		// TODO : prices' text can overflow
		event.setLine(3, Math.round(amount * itemWdouble * 100.0) / 100.0 + "      "
				+ Math.round(amount * itemWdouble * buySellRatio * 100.0) / 100.0);
		SignLogic.SaveSign(event);
		event.getPlayer().sendMessage("§5[DynEco]§f Sign placed and registered.");
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
//		if(event.getPlayer().isOp())
//		{
//			event.getPlayer().sendMessage("[DynEco] PlayerInteract.");
//		}
		
		if (event.isCancelled() || this.stockHandler == null) {
//			event.getPlayer().sendMessage("[DynEco] Cancelled: "+(event.isCancelled()?"true":"false")+" or stockHandler null: "+(this.stockHandler == null?"true":"false")+".");
			return;
		}

		if (!(event.getClickedBlock().getType() == Material.SIGN
				|| event.getClickedBlock().getType() == Material.WALL_SIGN
				|| event.getClickedBlock().getType() == Material.SIGN_POST)) // TODO:
																			// sign_post?
		{
//			event.getPlayer().sendMessage("[DynEco] Not a sign. Instead: "+event.getClickedBlock().getType());
			return;
		}
		
		Sign sign = (Sign) event.getClickedBlock().getState();

		if (sign == null || !SignLogic.isDynEcoSign(sign)) {
//			event.getPlayer().sendMessage("[DynEco] sign null or not dyneco. isDynEco: " + (SignLogic.isDynEcoSign(sign)?"true":"false"));
			return;
		}

		double buyPrice = 0;

		if (sign.getLine(1).equals("")) {
			return;
		}

		// Material mat = null;
		String[] lineOneSplit = sign.getLine(1).split(" ");

		if (lineOneSplit.length == 0) {
			return;
		}

		int amount = 1;
		ItemStack oneItem = null;

		try {
			if (lineOneSplit.length > 1) {
				int newAmount = Integer.parseInt(lineOneSplit[0]);

				if (newAmount > 0) {
					amount = newAmount;
				}

				oneItem = DynEconomy.essentials.getItemDb().get(lineOneSplit[1]);
			} else {
				oneItem = DynEconomy.essentials.getItemDb().get(lineOneSplit[0]);
			}
		} catch (Exception ex) {
			// this.respond(event.getPlayer(), "Please check item format!");
			event.getPlayer().sendMessage("§5[DynEco]§4 Couldn't resolve material!");
			return;
		}

		if (oneItem == null) {
			event.getPlayer().sendMessage("§5[DynEco]§4 Couldn't resolve material!");
			return;
		}

		String[] lineThreeSplit = sign.getLine(3).split(" ");

		if (lineThreeSplit.length < 2) {
			return;
		}

		buyPrice = Double.parseDouble(lineThreeSplit[0]);

		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			if (!event.getPlayer().hasPermission("dyneconomy.sign.sell")) {
				event.getPlayer().sendMessage("§5[DynEco]§4 You don't have permission to sell at this sign!");
				return;
			}

			User user = DynEconomy.essentials.getUser(event.getPlayer());
			PlayerInventory pi = event.getPlayer().getInventory();
			double sellPrice = Double.parseDouble(lineThreeSplit[lineThreeSplit.length - 1]);
			// if (event.getPlayer().getInventory().contains(allItems)) {
			if (event.getPlayer().getInventory().containsAtLeast(oneItem, amount)) {
				try {
					int remaining = amount;

					for (int i = 0; i < pi.getSize(); i++) {
						if (i >= pi.getSize()) {
							break;
						}

						ItemStack is = pi.getItem(i);
						if (is != null && is.getType().equals(oneItem.getType())
								&& is.getDurability() == oneItem.getDurability()) {
							if (is.getAmount() > remaining) {
								is.setAmount(is.getAmount() - remaining);
								remaining = 0;
								break;
							} else {
								remaining -= is.getAmount();
								pi.setItem(i, null);
							}
						}
					}

					event.getPlayer().updateInventory();
					this.stockHandler.depositStock(oneItem.getType(), (amount - remaining));
					user.giveMoney(new BigDecimal(sellPrice * (amount - remaining) / amount));
				} catch (MaxMoneyException e) {
					e.printStackTrace();
				}
			} else {
				user.sendMessage("§5[DynEco]§3 You don't have enough of that item: " + amount + " x "
						+ oneItem.getType() + (oneItem.getDurability() > 0 ? oneItem.getDurability() : ""));
			}

		} else if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			if (!event.getPlayer().hasPermission("dyneconomy.sign.buy")) {
				event.getPlayer().sendMessage("§5[DynEco]§4 You don't have permission to buy at this sign!");
				return;
			}
			// Bukkit.broadcastMessage( "[DynEco] Tried to buy for " +
			// totalBuyValue );
			User user = DynEconomy.essentials.getUser(event.getPlayer());
			BigDecimal bal = user.getMoney();

			if (bal.doubleValue() >= buyPrice) {
				if (this.stockHandler.getStock(oneItem.getType()) < amount) {
					user.sendMessage("§5[DynEco]§3 We don't have this much of that item. Consider selling it?");
				} else {
					int freeSpace = 0;

					for (ItemStack i : event.getPlayer().getInventory()) {
						if (i == null || i.getAmount() == 0) {
							freeSpace += oneItem.getType().getMaxStackSize();
						} else if (i.getType() == oneItem.getType()) {
							freeSpace += i.getType().getMaxStackSize() - i.getAmount();
						}
					}

					if (amount <= freeSpace) {
						user.takeMoney(new BigDecimal(buyPrice));
						this.stockHandler.withdrawStock(oneItem.getType(), amount);
						ItemStack itemsToAdd = new ItemStack(oneItem.getType(), amount);
						itemsToAdd.setDurability(oneItem.getDurability());
						event.getPlayer().getInventory().addItem(itemsToAdd);
						event.getPlayer().updateInventory();
					} else {
						event.getPlayer()
								.sendMessage("§5[DynEco]§3 You haven't got enough space left in your inventory!");
					}
				}
			} else {
				user.sendMessage("§5[DynEco]§3 You don't have enough money!");
			}
		}

	}
}
