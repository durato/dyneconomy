package hu.durato.dyneconomy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public class StockHandler {
	private ArrayList<StockData> stockData;

	StockTimerTask stockTimerTask;
	Timer stockTimer;

	FileConfiguration config;

	private void putStockToConfig(StockData item)
	{
		config.set("stockdata."+item.material.name()+".supply", item.supply);		
		config.set("stockdata."+item.material.name()+".demand", item.demand);			
		config.set("stockdata."+item.material.name()+".lastchangeat", item.lastChangeAt);			
		config.set("stockdata."+item.material.name()+".lastchangetime", item.lastChangeTime);
		config.set("stockdata."+item.material.name()+".lastaccesstime", item.lastAccessTime);
		config.set("stockdata."+item.material.name()+".decayhours", item.decayHours);			
		config.set("stockdata."+item.material.name()+".minprice", item.minPrice);		
		config.set("stockdata."+item.material.name()+".maxprice", item.maxPrice);
		config.set("stockdata."+item.material.name()+".changeratio", item.changeRatio);	
		DynEconomy.getInstance().saveConfig();
	}

	public StockHandler(FileConfiguration conf)
	{
		this.config = conf;
		if(conf==null)
		{
			Bukkit.getLogger().log(Level.SEVERE, "§5[DynEco] No config...");
			return;
		}
		//TODO: persistence, config, YML!

		stockData = new ArrayList<StockData>();

		for(Material mat : Material.values())
		{
			if(mat==null)
			{
				continue;
			}

			StockData item = null;

			if(!config.contains("stockdata."+mat.name()+".supply"))
			{
				item = new StockData(mat, 5000.0, 5000.0, 1, 10000.0, 1.0, 0.1);
				this.putStockToConfig(item);
			}
			else
			{
				item = this.stockDataFromConfig(mat);
			}

			stockData.add(item);
		}

		stockTimerTask= new StockTimerTask(this);


		stockTimer = new Timer();
		//stockTimer.schedule(stockDecayTimerTask, System.currentTimeMillis()+60*1000, (long)60*1000);
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(DynEconomy.getInstance(), stockTimerTask, 600, 600);
	}

	private StockData stockDataFromConfig(Material mat) {
		StockData ret = new StockData();
		ret.material = mat;
		ret.supply = config.getDouble("stockdata."+mat.name()+".supply");
		ret.demand = config.getDouble("stockdata."+mat.name()+".demand");
		ret.lastChangeAt = config.getDouble("stockdata."+mat.name()+".lastchangeat");
		ret.lastAccessTime = Date.from(Instant.now()); //TODO Date/epoch (long)
		ret.lastChangeTime = Date.from(Instant.now()); //TODO Date/epoch (long)
		ret.decayHours = config.getInt("stockdata."+mat.name()+".decayhours");
		ret.maxPrice = config.getDouble("stockdata."+mat.name()+".maxprice");
		ret.minPrice = config.getDouble("stockdata."+mat.name()+".minprice");
		ret.changeRatio = config.getDouble("stockdata."+mat.name()+".changeratio");

		return ret;
	}

	public double getStock(Material mat)
	{
		for(StockData stockItem : this.stockData)
		{
			if(stockItem == null || stockItem.material != mat)
			{
				continue;
			}

			return stockItem.supply;
		}

		return 0;
	}

	public void withdrawStock(Material mat, double amount)
	{
		for(StockData stockItem : this.stockData)
		{
			if(stockItem == null || stockItem.material != mat)
			{
				continue;
			}

			stockItem.supply -= amount;
			stockItem.lastAccessTime = Date.from(Instant.now());
			this.putStockToConfig(stockItem);
			//Bukkit.broadcastMessage("§5[DynEco]§6 Stock withdrawal of "+amount+"x"+mat);
		}
	}

	public void depositStock(Material mat, double amount)
	{
		for(StockData stockItem : this.stockData)
		{
			if(stockItem == null || stockItem.material != mat)
			{
				continue;
			}

			stockItem.supply += amount;
			stockItem.lastAccessTime = Date.from(Instant.now());
			this.putStockToConfig(stockItem);
			//Bukkit.broadcastMessage("§5[DynEco]§6 Stock deposit of "+amount+"x"+mat);
		}
	}

	public void checkPrices() {
		if(stockData == null)
		{
			return;
		}

		//System.out.println("[DynEco] checking supply and prices...");

		for(StockData stockItem : this.stockData)
		{
			if(stockItem == null || stockItem.material == null || stockItem.material==Material.AIR)
			{
				continue;
			}

			if(Math.abs(stockItem.lastChangeAt - stockItem.supply)>=stockItem.demand*stockItem.changeRatio)
			{
				//System.out.println("§5[DynEco]§6 Price change required for " + stockItem.material.name());
				this.changePriceAuto(stockItem);
			}
		}
	}

	private void doPriceDecay(StockData stockItem)
	{
		if(stockItem == null)
		{
			Bukkit.getLogger().log(Level.SEVERE, "[DynEco] doPriceDecay : stockItem was null");
			return;
		}

		if(DynEconomy.worth==null)
		{
			Bukkit.getLogger().log(Level.SEVERE, "[DynEco] doPriceDecay : getting Worth failed");
			return;
		}

		BigDecimal currentPriceBD = DynEconomy.worth.getPrice(new ItemStack(stockItem.material));

		if(currentPriceBD == null)
		{
			//System.out.println("[DynEco] worth for "+stockItem.material+" wasn't found.");
			return;
		}

		double currPrice = currentPriceBD.doubleValue();
		double newPrice = currPrice*(1.0-stockItem.changeRatio);

		if(stockItem.setPrice(newPrice))
		{
			this.putStockToConfig(stockItem);
			this.updateSigns(stockItem.material, newPrice);
		}
	}

	private void changePriceAuto(StockData stockItem) {		
		double currPrice = DynEconomy.essentials.getWorth().getPrice(new ItemStack(stockItem.material)).doubleValue();

		if(stockItem.supply<stockItem.lastChangeAt) // supply decreased
		{		
			if(stockItem.setPrice(currPrice*(1.0+2.0*stockItem.changeRatio*this.demandCompensation(stockItem))))
			{
				this.putStockToConfig(stockItem);
			}
			else
			{
				return;
			}
		}
		else if(stockItem.supply>stockItem.lastChangeAt) // supply increased
		{
			if(stockItem.setPrice(currPrice*(1.0-stockItem.changeRatio*this.demandCompensation(stockItem))))
			{
				this.putStockToConfig(stockItem);
			}
			else
			{
				return;
			}
		}

		double newPrice = Math.round(DynEconomy.essentials.getWorth().getPrice(new ItemStack(stockItem.material)).doubleValue()*100)/100.0;
		Bukkit.broadcast("§5[DynEco]§7 Price changed! Item: §4" + stockItem.material.name() +"§7 Price: " + Math.round(currPrice*100)/100.0 + " -> "+newPrice, "bukkit.broadcast.user"); //TODO: publicly visible

		this.updateSigns(stockItem.material, newPrice);
	}

	private double demandCompensation(StockData stockItem) {
		//=1+(1-SQRT(ABS(diff^2-demand^2))/demand)/5
		double diff = Math.abs(stockItem.demand-stockItem.supply);
		double corr_raw = 1.0 + (1.0-Math.sqrt(Math.abs(diff*diff - stockItem.demand*stockItem.demand))/stockItem.demand/5.0);
		
		if(corr_raw>1+stockItem.changeRatio)
		{
			return 1.0+stockItem.changeRatio;
		}
		else if(corr_raw<1.0-stockItem.changeRatio)
		{
			return 1.0-stockItem.changeRatio;
		}
		
		return corr_raw;
	}

	private void updateSigns(Material material, double newPrice)
	{
		if(this.config == null || this.config.getConfigurationSection("signdata")==null)
		{
			//Bukkit.getLogger().log(Level.SEVERE, "[DynEco] updateSigns: config error");
			return;
		}

		for(String world : this.config.getConfigurationSection("signdata").getKeys(false))
		{
			for(String pos : this.config.getConfigurationSection("signdata."+world+".sign.pos").getKeys(false))
			{				
				Material mat = null;
				String[] lineOneSplit = config.getString("signdata."+world+".sign.pos."+pos+".items").split(" ");

				if(lineOneSplit.length==0)
				{
					return;
				}

				//mat = Material.matchMaterial(lineOneSplit[lineOneSplit.length-1]);
				try {
					mat = DynEconomy.essentials.getItemDb().get(lineOneSplit[lineOneSplit.length-1]).getType();
				} catch (Exception e) {
					Bukkit.getLogger().log(Level.SEVERE, "[DynEco] getItemDb failed");
					//e.printStackTrace();
					return;
				}

				if( mat != material)
				{
					continue;
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

				double buyPrice = newPrice; //worth.getPrice(new ItemStack(mat)).doubleValue();
				double sellPrice = buyPrice * config.getDouble("settings.buysellratio");

				String[] xyzStr = pos.split("/");

				if(xyzStr.length!=3)
				{
					return;
				}

				int x = Integer.parseInt(xyzStr[0]);
				int y = Integer.parseInt(xyzStr[1]);
				int z = Integer.parseInt(xyzStr[2]);

				Block b = Bukkit.getWorld(world).getBlockAt(x, y, z);
				if( b.getType() == Material.SIGN || b.getType()==Material.WALL_SIGN )
				{
					Sign sign = (Sign) b.getState();

					if(sign==null)
					{
						System.out.println("[DynEco] sign was null...");						
					}

					sign.setLine(3, Math.round(amount * buyPrice*100.0)/100.0 + "      " + Math.round(amount * sellPrice*100.0)/100.0 );
					sign.update();
					//config.set("signdata."+key+".sign.pos."+pos+".items",sign.getLine(2));
					DynEconomy.getInstance().saveConfig();					
				}
				else
				{
					config.set("signdata."+world+".sign.pos."+pos, null);
				}
				//event.setLine(3, amount * itemWdouble + "      " + amount * itemWdouble * buySellRatio );
			}
		}
	}

	public void checkDecay() {
		boolean anyChanges = false;
		for(StockData stockItem : this.stockData)
		{
			if(stockItem == null)
			{
				continue;
			}

			long diff = Date.from(Instant.now()).getTime() - stockItem.lastAccessTime.getTime();
			//long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
			long hours = TimeUnit.MILLISECONDS.toHours(diff);

//			if(stockItem.material == Material.STONE)
//			{
//				Bukkit.broadcastMessage("§5[DynEco]§6 "+stockItem.material.name()+" diff = " + minutes + " minutes.");
//			}

			if(hours >= (long)stockItem.decayHours)
			{
//				if(stockItem.material == Material.STONE)
//				{
//					Bukkit.broadcastMessage("§5[DynEco]§6 Price decay for §7"+stockItem.material.name()+"§r necessary as exceeds "+(long)stockItem.decayHours);
//				}

				this.doPriceDecay(stockItem);
				anyChanges = true;
			}
			//Bukkit.broadcastMessage("§5[DynEco]§6 Stock deposit of "+amount+"x"+mat);
		}

		if(anyChanges)
		{
			DynEconomy.getInstance().saveConfig();
		}
	}

	public void updateSigns() {
		if(this.config.getConfigurationSection("signdata") == null)
		{
			return;
		}
		
		for(String world : this.config.getConfigurationSection("signdata").getKeys(false))
		{
			for(String pos : this.config.getConfigurationSection("signdata."+world+".sign.pos").getKeys(false))
			{		
				String[] xyzStr = pos.split("/");

				if(xyzStr.length!=3)
				{
					return;
				}
				
				Material mat = null;
				String lineOne = config.getString("signdata."+world+".sign.pos."+pos+".items");
				
				String[] lineOneSplit = lineOne.split(" ");

				if(lineOneSplit.length==0)
				{
					return;
				}

				//mat = Material.matchMaterial(lineOneSplit[lineOneSplit.length-1]);
				try {
					mat = DynEconomy.essentials.getItemDb().get(lineOneSplit[lineOneSplit.length-1]).getType();
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
				
				BigDecimal price = DynEconomy.worth.getPrice(new ItemStack(mat));
				
				if(price == null)
				{
					Bukkit.getLogger().log(Level.SEVERE, "[DynEco] StockHandler, updatesigns: price was null.");
					continue;
				}

				double buyPrice = price.doubleValue();
				double sellPrice = buyPrice * config.getDouble("settings.buysellratio");

				int x = Integer.parseInt(xyzStr[0]);
				int y = Integer.parseInt(xyzStr[1]);
				int z = Integer.parseInt(xyzStr[2]);

				Block b = Bukkit.getWorld(world).getBlockAt(x, y, z);
				if( b.getType() == Material.SIGN || b.getType()==Material.WALL_SIGN )
				{
					Sign sign = (Sign) b.getState();

					if(sign==null)
					{
						System.out.println("[DynEco] sign was null...");						
					}

					sign.setLine(3, Math.round(amount * buyPrice*100.0)/100.0 + "      " + Math.round(amount * sellPrice*100.0)/100.0 );
					sign.update();
					//config.set("signdata."+key+".sign.pos."+pos+".items",sign.getLine(2));
					DynEconomy.getInstance().saveConfig();					
				}
				else
				{
					config.set("signdata."+world+".sign.pos."+pos, null);
				}
				//event.setLine(3, amount * itemWdouble + "      " + amount * itemWdouble * buySellRatio );
			}
		}
	}

	public void forceDecay() {
		for(StockData stockItem : this.stockData)
		{
			if(stockItem == null)
			{
				continue;
			}
			
			this.doPriceDecay(stockItem);
		}
	}
}
