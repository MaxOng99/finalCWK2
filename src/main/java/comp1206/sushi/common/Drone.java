package comp1206.sushi.common;

import java.io.Serializable;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import comp1206.sushi.server.IngredientStockManager;
import comp1206.sushi.server.Server;

public class Drone extends Model implements Runnable, Serializable{
	
	private static final long serialVersionUID = -333372487038185433L;
	private volatile Number progress;
	private Number speed;
	private Number capacity;
	private Number battery;
	private volatile Postcode source;
	private volatile Postcode destination;
	private Postcode restaurantPostcode;
	private volatile boolean removedFromServer;
	private volatile boolean restockStatus; 
	private volatile String status;
	private IngredientStockManager ingredientManager;
	private BlockingQueue<Order> orderQueue;
	private BlockingQueue<Ingredient> ingredientQueue;
	private Thread droneThread;
	private Server server;
	
	public Drone(Number speed) {
		this.speed = speed;
		this.capacity = 1;
		this.battery = 100;
		this.status = "Idle";
		this.progress = null;
		this.droneThread = new Thread(this);
		droneThread.setName(getName());
		this.removedFromServer = false;
		this.restockStatus = true;
	}
	
	public void setServer(Server server) {
		this.server = server;
	}
	
	public void setRestaurantPostcode(Postcode restaurantPostcode) {
		this.restaurantPostcode = restaurantPostcode;
		droneThread.start();
	}
	
	public void setOrderQueue(BlockingQueue<Order> serverOrderQueue) {
		orderQueue = serverOrderQueue;
		
	}
	
	public void setIngredientStockManager(IngredientStockManager serverIngredientManager) {
		this.ingredientManager = serverIngredientManager;
		ingredientQueue = ingredientManager.getIngredientRestockQueue();
	}
	
	public Number getProgress() {
		return progress;
	}
	
	public void setProgress(Number progress) {
		this.notifyUpdate();
		this.progress = progress;
	}
	
	public Number getSpeed() {
		return speed;
	}
	
	public void setSpeed(Number speed) {
		this.notifyUpdate("Speed", this.speed, speed);
		this.speed = speed;
	}
	
	public Postcode getSource() {
		return source;
	}

	public void setSource(Postcode source) {
		this.notifyUpdate("Source", this.source, source);
		this.source = source;
	}
	
	public boolean getRestockStatus() {
		return restockStatus;
	}
	
	public void setRestockStatus(boolean status) {
		restockStatus = status;
	}
	
	public Postcode getDestination() {
		return destination;
	}

	public void setDestination(Postcode destination) {
		this.notifyUpdate("Destination", this.destination, destination);
		this.destination = destination;
	}
	
	public Number getCapacity() {
		return capacity;
	}

	public void setCapacity(Number capacity) {
		this.notifyUpdate("Capacity", this.capacity, capacity);
		this.capacity = capacity;
	}

	public Number getBattery() {
		return battery;
	}

	public void setBattery(Number battery) {
		this.notifyUpdate("Battery", this.battery, battery);
		this.battery = battery;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		notifyUpdate("status",this.status,status);
		this.status = status;
	}
	
	public void deleteFromServer() {
		droneThread.interrupt();
		removedFromServer = true;
	}
	
	@Override
	public String getName() {
		return "Drone (" + getSpeed() + " speed)";
	}
	
	@Override
	public void run() {
		while (true) {
			if(!removedFromServer) {
				try {
					restockIngredient();
					deliverOrder();
				}
				catch(InterruptedException e) {
					return;
				}
			}
			else {
				return;
			}
		}
	}
	
	public void restockIngredient() throws InterruptedException{
		
			Ingredient ingredientTaken = null;
			ingredientTaken = ingredientQueue.poll(1, TimeUnit.SECONDS);
			
			if (ingredientTaken == null) {
				return;
			}
			else {
				performRestockProcess(ingredientTaken);
			}
		
		
	}
	
	public void deliverOrder() throws InterruptedException{
		if (getStatus().equals("Idle")) {
			Order order = null;
			order = orderQueue.poll(1, TimeUnit.SECONDS);
		
			if (order == null) {
				return;
			}
			else {
				performDeliveryProcess(order);
			}	
		}
	}
	
	public void performDeliveryProcess(Order order) {
		if (restaurantPostcode.getName().equals(order.getUser().getPostcode().getName())) {
			order.setStatus("Complete");
			this.setStatus("Idle");
			server.getDP().writeOrders(server.getOrders());
		}
		
		else {
			this.setSource(restaurantPostcode);
			this.setDestination(order.getUser().getPostcode());
			float distanceTravelled = 0;
			double totalDistance = (double) this.getDestination().getDistance();
	        this.setStatus("Delivering Order...");
	        while (distanceTravelled < totalDistance) {
	        	this.setProgress(Math.round((distanceTravelled*100)/totalDistance));
				distanceTravelled += this.getSpeed().intValue();
				try {
					Thread.sleep(1000);
				}
				catch(InterruptedException e) {
					return;
				}
	        }
	        
	        this.setProgress(100);
			this.setStatus("Returning To Restaurant");
			this.setSource(this.getDestination());
			this.setDestination(restaurantPostcode);
	        distanceTravelled = 0;
	        order.setStatus("Complete");
			server.getDP().writeOrders(server.getOrders());
			
	        while (distanceTravelled < totalDistance) {
	        	this.setProgress(Math.round((distanceTravelled*100)/totalDistance));
				distanceTravelled += this.getSpeed().intValue();
				try {
					Thread.sleep(1000);
				}
				catch(InterruptedException e) {
					return;
				}
	        }
	        
			this.setProgress(null);
			this.setSource(null);
			this.setDestination(null);
			this.setStatus("Idle");
	    }
		
	}	
	
	public void performRestockProcess(Ingredient ingredientTaken) {
		ingredientTaken.setIngredientAvailability(false);
		int currentStockValue = (int)ingredientManager.getStock(ingredientTaken);
		int restockThreshold = (int) ingredientTaken.getRestockThreshold();
		int restockAmount = (int) ingredientTaken.getRestockAmount();
		this.setSource(restaurantPostcode);
		this.setDestination(ingredientTaken.getSupplier().getPostcode());
		double totalDistance = (double) this.getDestination().getDistance();
		float distanceTravelled = 0;
		this.setStatus("Fetching " + ingredientTaken + "...");
		while (currentStockValue < restockThreshold) {   
	       
			while (distanceTravelled < totalDistance) {
				this.setProgress(Math.round((distanceTravelled*100)/totalDistance));
				distanceTravelled += this.getSpeed().intValue();
				try {
					Thread.sleep(1000);
				}
				catch(InterruptedException e) {
					return;
				}
			}
			
			this.setProgress(100);
			this.setStatus("Returning To Restaurant");
			this.setSource(this.getDestination());
			this.setDestination(restaurantPostcode);
			
			distanceTravelled = 0;
	        
	        while (distanceTravelled < totalDistance) {
	        	this.setProgress(Math.round((distanceTravelled*100)/totalDistance));
				distanceTravelled += this.getSpeed().intValue();
				try {
					Thread.sleep(1000);
				}
				catch(InterruptedException e) {
					return;
				}
			}
	        
	        this.setProgress(100);
			this.setProgress(null);
			this.setSource(null);
			this.setDestination(null);
			this.setStatus("Idle");
			
	        ingredientManager.directRestock(ingredientTaken, currentStockValue + restockAmount);
	        currentStockValue += restockAmount;
	        server.getDP().writeIngredientManager(ingredientManager);
		}
		
		ingredientTaken.setIngredientAvailability(true);
		ingredientTaken.setRestockStatus(false);
		this.setStatus("Idle");
	}
}
