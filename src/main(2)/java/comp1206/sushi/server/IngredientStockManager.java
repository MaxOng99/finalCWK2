package comp1206.sushi.server;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import comp1206.sushi.common.Drone;
import comp1206.sushi.common.Ingredient;


public class IngredientStockManager {
	
	private Server server;
	private Random restockTime = new Random();
	private Map<Ingredient, Number> ingredientStock;
	private BlockingQueue<Ingredient> ingredientRestockQueue;
	private Object directRestockLock = new Object();
	
	public IngredientStockManager(Server server) {
		this.server = server;
		ingredientStock = new ConcurrentHashMap<>();
		ingredientRestockQueue = new LinkedBlockingQueue<>();
	}
	
	public void clearAllData() {
		ingredientStock.clear();
		ingredientRestockQueue.clear();
	}
	public void initializeStockFromConfig(Map<Ingredient, Number> configStock) {
		ingredientStock = configStock;
		for (Entry<Ingredient, Number> currentEntry: ingredientStock.entrySet()) {
			Ingredient currentIngredient = currentEntry.getKey();
			int currentStock = (int) currentEntry.getValue();
			if (currentStock < (int) currentIngredient.getRestockThreshold()) {
				try {
					currentIngredient.setIngredientAvailability(false);
					currentIngredient.setRestockStatus(true);
					ingredientRestockQueue.put(currentIngredient);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else {
				currentIngredient.setIngredientAvailability(true);
			}
		}
	}
	
	public void addIngredientToStockManager(Ingredient ingredient, Number quantity) {
		ingredientStock.put(ingredient, quantity);
	}
	
	public void removeIngredientFromStockManager(Ingredient ingredient) {
		ingredientStock.remove(ingredient);
	}
	
	public Map<Ingredient, Number> getIngredientStockLevel() {
		return ingredientStock;
	}
	
	public Number getStock(Ingredient ingredient) {
		return ingredientStock.get(ingredient);
	}
	
	public BlockingQueue<Ingredient> getIngredientRestockQueue() {
		return ingredientRestockQueue;
	}
	
	public void directRestock(Ingredient ingredient, Number quantitiy) {
		synchronized(directRestockLock) {
			ingredientStock.replace(ingredient, quantitiy);
		}	
	}
	
	public void setStock(Ingredient ingredient, Number quantity){	
		int ingredientQuantity = (int) ingredientStock.get(ingredient);
		int newIngredientQuantity = ingredientQuantity + (int) quantity;
		
		if (newIngredientQuantity < 0) {
			ingredientStock.replace(ingredient, (int)0);
		}
		else {
			ingredientStock.replace(ingredient, newIngredientQuantity);
		}
		if (newIngredientQuantity < (int) ingredient.getRestockThreshold()) {
			System.out.println("gonna go restock ingredients if condiition tru");
			ingredient.setIngredientAvailability(false);
			try {
				if (ingredientRestockQueue.contains(ingredient) || ingredient.beingRestocked() == true) {
					System.out.println(ingredient + " being restocked already");
				}
				else {
					ingredient.setRestockStatus(true);
					ingredientRestockQueue.put(ingredient);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			ingredient.setIngredientAvailability(true);
		}
	}
	
	public void requestRestock(Ingredient ingredient) {
		try {
			if (!ingredientRestockQueue.contains(ingredient) && ingredient.beingRestocked() == false) {
				ingredient.setRestockType("Extra");
				ingredientRestockQueue.put(ingredient);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}