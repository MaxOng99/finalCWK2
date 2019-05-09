package comp1206.sushi.server;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import comp1206.sushi.common.Dish;

public class DishStockManager implements Serializable{

	private static final long serialVersionUID = 2541673786741240083L;
	private Map<Dish, Number> dishStock;
	private List<Dish> dishes;
	private BlockingQueue<Dish> dishRestockQueue;

	
	public DishStockManager() {
		dishRestockQueue = new LinkedBlockingQueue<>();
		dishes = Collections.synchronizedList(new ArrayList<>());
		dishStock = new ConcurrentHashMap<>();
	}
	
	public void clearAllData() {
		dishStock.clear();
		dishRestockQueue.clear();
		dishes.clear();
	}
	
	public void initialRestockProcess() {
		for (Entry<Dish, Number> currentEntry: dishStock.entrySet()) {
			Dish currentDish = currentEntry.getKey();
			
			if (currentDish.beingRestocked() == true) {
				try {
					dishRestockQueue.put(currentDish);
					
				}
				catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			else {
				int currentDishQuantity = (int)currentEntry.getValue();
				if (shouldRestock(currentDish, currentDishQuantity)) {
					try {
						dishRestockQueue.put(currentDish);
					}
					catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void initializeDishStock(Map<Dish, Number> configStock) {
		dishStock = configStock;
		dishes.addAll(dishStock.keySet());
		initialRestockProcess();
		
	}
	
	public List<Dish> getDishes() {
		return dishes;
	}
	
	public void addDish(Dish dish, Number quantity) {
		dishes.add(dish);
		dishStock.put(dish, quantity);
		setStock(dish, quantity);
	}
	
	public void removeDish(Dish dish) {
		dishes.remove(dish);
		dishStock.remove(dish);
	}
	
	public int getStock(Dish dish) {
		return (int) dishStock.get(dish);
	}
	
	public Map<Dish, Number> getDishStockLevels() {
		return dishStock;
	}
	
	public BlockingQueue<Dish> getDishRestockQueue() {
		return dishRestockQueue;
	}
	
	public void setStock(Dish dish, Number quantity){	
		synchronized(this) {
			int dishQuantity = (int) dishStock.get(dish);
			int newDishQuantity = dishQuantity + (int) quantity;
			
			if (newDishQuantity < 0) {
				newDishQuantity = 0;
				dishStock.replace(dish, newDishQuantity);
			}
			
			dishStock.replace(dish, newDishQuantity);
			
			if (shouldRestock(dish, newDishQuantity)) {
				try {
					dishRestockQueue.put(dish);
				}
				catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void directRestock(Dish dish, int amountToRestock) {
		dishStock.replace(dish, amountToRestock);
	}
	
	public boolean shouldRestock(Dish dish, int currentQuantity) {
		
		if (currentQuantity < (int)dish.getRestockThreshold()) {
			if (dishRestockQueue.contains(dish)) {
				return false;
			} 
			
			else {
				return true;
			}
		}
		else {
			return false;
		}
	}
	
	
}