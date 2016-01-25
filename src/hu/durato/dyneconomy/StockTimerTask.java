package hu.durato.dyneconomy;
import java.util.TimerTask;

public class StockTimerTask extends TimerTask{
	StockHandler stockHandler;
	
	public StockTimerTask(StockHandler stockHandler) {
		this.stockHandler = stockHandler;
	}
	
	@Override
    public void run() {
        //System.out.println("Timer task started at:"+new Date());
        completeTask();
        //System.out.println("Timer task finished at:"+new Date());
    }
 
    private void completeTask() {
    	stockHandler.checkPrices();
    	stockHandler.checkDecay();
    }
}
