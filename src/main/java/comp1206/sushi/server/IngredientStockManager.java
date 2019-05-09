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

import comp1206.sushi.common.Ingredient;

public class IngredientStockManager implements Serializable{
	
	private static final long serialVersionUID = 5913378645569913125L;
	private Map<Ingredient, Number> ingredientStock;
	private BlockingQueue<Ingredient> ingredientRestockQueue;
	private List<Ingredient> ingredients; 

	
	public IngredientStockManager() {
		ingredientStock = new ConcurrentHashMap<>();
		ingredientRestockQueue = new LinkedBlockingQueue<>();
		ingredients = Collections.synchronizedList(new ArrayList<>());
	}
	
	public void clearAllData() {
		ingredientStock.clear();
		ingredientRestockQueue.clear();
		ingredients.clear();
	}
	
	public void initialRestockProcess() {
		for (Entry<Ingredient, Number> currentEntry: ingredientStock.entrySet()) {
			Ingredient currentIngredient = currentEntry.getKey();
			int currentIngredientQuantity = (int)currentEntry.getValue();
			
			if (currentIngredient.beingRestocked() == true) {
				try {
					ingredientRestockQueue.put(currentIngredient);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			else {
				if (shouldRestock(currentIngredient, currentIngredientQuantity)) {
					try {
						ingredientRestockQueue.put(currentIngredient);
					}
					catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
	}
	
	public void initializeIngredientStock(Map<Ingredient, Number> configStock) {
		ingredientStock = configStock;
		ingredients.addAll(ingredientStock.keySet());
		initialRestockProcess();
	}
	
	public List<Ingredient> getIngredients() {
		return ingredients;
	}
	public void addIngredient(Ingredient ingredient, Number quantity) {
		ingredients.add(ingredient);
		ingredientStock.put(ingredient, quantity);
		setStock(ingredient, quantity);
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
	
	public boolean shouldRestock(Ingredient ingredient, int currentQuantity) {
		if (currentQuantity < (int)ingredient.getRestockThreshold()) {
			if (ingredientRestockQueue.contains(ingredient)) {
				return false;
			}
			
			else if (ingredient.beingRestocked() == true) {
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
	
	public void directRestock(Ingredient ingredient, Number quantitiy) {
		synchronized(this) {
			ingredientStock.replace(ingredient, quantitiy);
		}	
	}
	
	public void setStock(Ingredient ingredient, Number quantity){	
		synchronized(this) {
			int ingredientQuantity = (int) getStock(ingredient);
			int newIngredientQuantity = ingredientQuantity + (int) quantity;
			
			if (newIngredientQuantity < 0) {
				ingredientStock.replace(ingredient, (int)0);
			}
			else {
				ingredientStock.replace(ingredient, newIngredientQuantity);
			}
			
			if (shouldRestock(ingredient, newIngredientQuantity)) {
				try {
					ingredient.setRestockStatus(true);
					ingredientRestockQueue.put(ingredient);
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}