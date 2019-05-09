package comp1206.sushi.common;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import comp1206.sushi.server.DishStockManager;
import comp1206.sushi.server.Server;

public class Staff extends Model implements Runnable, Serializable{

	private static final long serialVersionUID = -1247525323219882003L;
	private volatile boolean shouldRestock;
	private volatile boolean isRecoveringFatigue;
	private volatile boolean removedFromServer;
	private Server server;
	private String name;
	private DishStockManager stockManager;
	private volatile String status;
	private volatile Number fatigue;
	private Thread kitchenStaff;
	private Thread monitorFatigueThread;
	private BlockingQueue<Dish> dishRestockQueue;
	private Random restockTime = new Random();
	
	public Staff(String name, Server server) {
		this.server = server;
		this.isRecoveringFatigue = false;
		this.setName(name);
		this.setFatigue(0);
		this.setStatus("Idle");
		removedFromServer = false;
	}
	
	public void deleteFromServer() {
		kitchenStaff.interrupt();
		monitorFatigueThread.interrupt();
		removedFromServer = true;
	}
	
	public void setRestockStatus(boolean status) {
		this.shouldRestock = status;
	}
	
	public boolean shouldRestock() {
		return this.shouldRestock;
	}
	public String getName() {
		return name;
	}

	public void setDishStckManager(DishStockManager serverStockManager) {
		stockManager = serverStockManager;
		dishRestockQueue = stockManager.getDishRestockQueue();
		kitchenStaff = new Thread(this);
		kitchenStaff.setName(getName());
		monitorFatigueThread = new Thread(new MonitorFatigueLevel());
		monitorFatigueThread.setName("Fatigue Monitor");
		kitchenStaff.start();
		monitorFatigueThread.start();
	}
	
	public void setName(String name) {
		this.notifyUpdate("Name", this.name, name);
		this.name = name;
	}

	public Number getFatigue() {
		return fatigue;
	}
	
	public void setFatigue(Number fatigue) {
		this.notifyUpdate();
		this.fatigue = fatigue;
	}
	
	public int calculateFatigue(int fatigueNumber) {
		int fatigue = (int)this.fatigue + fatigueNumber;
		if(fatigue >= 100) {
			return 100;
		}
		else {
			return fatigue;
		}
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		notifyUpdate("Staff status",this.status,status);
		this.status = status;
	}
	
	@Override
	public void run() {
		while(!Thread.currentThread().isInterrupted()) {
			if (!removedFromServer) {
				makeDishes();
			}
			else {
				return;
			}
		}
	}
	
	public void makeDishes() {
		
		if (!isRecoveringFatigue) {
			try {
				
				Dish dishTaken = dishRestockQueue.take();
				
				if (dishTaken.beingRestocked() == true) {
					int currentStockValue = stockManager.getStock(dishTaken);
					int restockThreshold = (int) dishTaken.getRestockThreshold();
					int restockAmount = dishTaken.getRestockAmount().intValue();
					
					while(currentStockValue < restockThreshold) {
						this.setStatus("Restocking " + dishTaken + "...");
						this.setFatigue(calculateFatigue(restockAmount));
						Thread.sleep(restockTime.nextInt(40001) + 30000);
						stockManager.directRestock(dishTaken, restockAmount + currentStockValue);
						currentStockValue += restockAmount;
					}
				}
				
				else {
					dishTaken.setRestockStatus(true);
					server.getDP().writeDishManager(stockManager);
					Map<Ingredient, Number> recipe = dishTaken.getRecipe();
					int currentStockValue = stockManager.getStock(dishTaken);
					int restockThreshold = (int) dishTaken.getRestockThreshold();
					int restockAmount = dishTaken.getRestockAmount().intValue();
					while(currentStockValue < restockThreshold) {
						
						for (Entry<Ingredient, Number> current: recipe.entrySet()) {
							int deductedAmount = current.getValue().intValue() * restockAmount;
							server.setStock(current.getKey(), -deductedAmount);
						}
						
						this.setStatus("Restocking " + dishTaken + "...");
						this.setFatigue(calculateFatigue(restockAmount));
						Thread.sleep(restockTime.nextInt(40001) + 30000);
						stockManager.directRestock(dishTaken, restockAmount + currentStockValue);
						currentStockValue += restockAmount;
					}
				}
				
				dishTaken.setRestockStatus(false);	
				this.setStatus("Idle");
				server.getDP().writeDishManager(stockManager);
				
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	private class MonitorFatigueLevel implements Runnable {

		@Override
		public void run() {
			while(!Thread.currentThread().isInterrupted()) {
				int fatigueLevel = Staff.this.fatigue.intValue();
				int currentFatigue = fatigueLevel;
				
				if (fatigueLevel == 100) {
					isRecoveringFatigue = true;
					while (currentFatigue > 0) {
						setFatigue(--currentFatigue);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							
						}
					}
					isRecoveringFatigue = false;
				}
				
				else {
					while (currentFatigue > 0 && getStatus().equals("Idle")) {
						setFatigue(--currentFatigue);
						try {
							Thread.sleep(1000);
						}
						catch(InterruptedException e) {
							
						}
					}
				}
			}
		}
	}
}
