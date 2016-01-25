package hu.durato.dyneconomy;

import java.time.Instant;
import java.util.Date;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class StockData {
	Material material = Material.AIR;
	double demand = 5000;		// the ideal amount
	double supply = 5000;		// the current amount
	double lastChangeAt = 5000;	// obvious.
	Date lastChangeTime = Date.from(Instant.now());
	Date lastAccessTime = Date.from(Instant.now());
	int decayHours = 1;			// if decayHours time has elapsed and prices haven't changed, they will.
	double maxPrice = 10000;	// price will stop going higher than maxPrice
	double minPrice = 1;		// and will be kept at least minPrice
	
	/*
	 * Change ratio:
	 * if abs(supply-lastChangeAt)>=demand*changeRatio then price changes by changeRatio.
	 * if prices are automatically changed because noone buys or sells, it's changed by changeRatio.
	 */
	double changeRatio = 0.1; // 10%
	
	public StockData(Material mat, double demand, double supply, int decayHours, double maxPrice, double minPrice, double changeRatio)
	{
		this.material = mat;
		this.demand = demand;
		this.supply = supply;
		this.lastChangeAt = supply;
		this.decayHours = decayHours;
		this.maxPrice = maxPrice;
		this.minPrice = minPrice;
		this.changeRatio = changeRatio;
	}

	public StockData() {
		// TODO Auto-generated constructor stub
	}

	public boolean setPrice(double desiredPrice)
	{
		if(desiredPrice<this.minPrice || desiredPrice>this.maxPrice)
		{
			return false;
		}
		
		DynEconomy.worth.setPrice(new ItemStack(this.material), desiredPrice);
		this.lastChangeAt = this.supply;
		this.lastChangeTime = Date.from(Instant.now());
		this.lastAccessTime = Date.from(Instant.now());
		return true;
	}
}
