package comp1206.sushi.server;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Ingredient;


public class DishStockManager {
	
	private Server server;
	private IngredientStockManager ingredientManager;
	private Map<Dish, Number> dishStock;
	private BlockingQueue<Dish> dishRestockQueue;
	private BlockingQueue<Dish> lackIngredientList;
	private Thread checkAbleToRestock;
	private Object dishLock = new Object();
	
	public DishStockManager(IngredientStockManager serverIngredientManager, Server server) {
		this.server = server;
		ingredientManager = serverIngredientManager;
		dishRestockQueue = new LinkedBlockingQueue<>();
		lackIngredientList = new LinkedBlockingQueue<>();
		checkAbleToRestock = new Thread(new CheckAbleToRestock());
		checkAbleToRestock.start();
	}
	
	public void clearAllData() {
		dishStock.clear();
		dishRestockQueue.clear();
		lackIngredientList.clear();
	}
	
	public void addDishToStockManager(Dish dish, Number quantity) {
		dishStock.put(dish, quantity);
	}
	
	public void removeDishFromStockManager(Dish dish) {
		dishStock.remove(dish);
	}
	
	public void initializeStockFromConfig(Map<Dish, Number> configStock) {
		dishStock = configStock;
		
		for (Entry<Dish, Number> currentEntry: dishStock.entrySet()) {
			Dish currentDish = currentEntry.getKey();
			int currentDishStock = (int) currentEntry.getValue();
			int restockThreshold = (int) currentDish.getRestockThreshold();
			if (currentDishStock < restockThreshold) {
				if (ableToRestock(currentDish) == true) {
					try {
						currentDish.setRestockStatus(true);
						dishRestockQueue.put(currentDish);
						System.out.println("Added " + currentDish + " to Restocking Queue");
					}
					
					catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				else {
					try {
						lackIngredientList.put(currentDish);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
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
	
	public boolean ableToRestock(Dish dish) {
		
		Map<Ingredient, Number> recipe = dish.getRecipe();
		boolean ableToRestock = true;
		for (Entry<Ingredient, Number> currentEntry: recipe.entrySet()) {
			Ingredient currentIngredient = currentEntry.getKey();
			int quantityNeeded = (int)currentEntry.getValue();
			if ((int)ingredientManager.getStock(currentIngredient) < (int)dish.getRestockAmount()*quantityNeeded) {
				if (currentIngredient.beingRestocked() == false) {
					ingredientManager.requestRestock(currentIngredient);
				}
				ableToRestock = false;
			}
		}
		return ableToRestock;
	}
	
	public void checkLackingIngredient() {
		
	}
	public void requireExtraStock(Dish dish) {
		try {
			if (ableToRestock(dish) == false) {
				System.out.println("Not enough ingredients");
				try {
					lackIngredientList.put(dish);
				}
				catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			else {
				if (dishRestockQueue.contains(dish)) {
					System.out.println(dish + " is already added to restock queue");
				}
				
				else if (dish.beingRestocked() == true) {
					System.out.println(dish + " is being restocked");
				}
				else {
					dishRestockQueue.put(dish);
					System.out.println("Added " + dish +" to restock queue");
					dish.setRestockStatus(true);
				}
			} 
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void setStock(Dish dish, Number quantity){	
		synchronized(dishLock) {
			int dishQuantity = (int) dishStock.get(dish);
			int newDishQuantity = dishQuantity + (int) quantity;
			
			if (newDishQuantity < 0) {
				dishStock.replace(dish, 0);
			}
			else {
				dishStock.replace(dish, newDishQuantity);
			}
			
			if (newDishQuantity < (int) dish.getRestockThreshold()) {
				try {
					if (ableToRestock(dish) == false) {
						System.out.println("Not enough ingredients");
						lackIngredientList.put(dish);
					}
					else {
						if (dishRestockQueue.contains(dish) || dish.beingRestocked() == true) {
							System.out.println(dish + " is already added to restock queue");
						}
						else {
							dishRestockQueue.put(dish);
							dish.setRestockStatus(true);
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public BlockingQueue<Dish> getLackIngredientList() {
		return lackIngredientList;
	}
	
	class CheckAbleToRestock implements Runnable {

		@Override
		public void run() {
			while(true) {
				try {
					Dish dish = lackIngredientList.take();
					if (ableToRestock(dish)) {
						dishRestockQueue.put(dish);
					}
					else {
						lackIngredientList.put(dish);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}