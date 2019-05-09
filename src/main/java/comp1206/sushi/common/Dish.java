package comp1206.sushi.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Ingredient;

public class Dish extends Model implements Serializable{
	
	private static final long serialVersionUID = -350158669810498848L;
	private volatile boolean beingRestocked;
	private volatile boolean availability;
	private Map <Ingredient,Number> recipe;
	private String name;
	private String description;
	private Number price;
	private Number restockThreshold;
	private Number restockAmount;
	
	public Dish(String name, String description, Number price, Number restockThreshold, Number restockAmount) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.restockThreshold = restockThreshold;
		this.restockAmount = restockAmount;
		this.recipe = new HashMap<Ingredient,Number>();
	}
	
	public void addIngredientToRecipe(Ingredient ingredient, Number quantity) {
		Map<Ingredient, Number> oldRecipe = new HashMap<>();
		oldRecipe.putAll(getRecipe());
		if (quantity.intValue() == 0) {
			recipe.remove(ingredient);
		}
		else {
			recipe.put(ingredient, quantity);
		}
		this.notifyUpdate("Recipe", oldRecipe.toString(), this.recipe.toString());
	}
	
	public void removeIngredientFromRecipe(Ingredient ingredient) {
		Map<Ingredient, Number> oldRecipe = new HashMap<>();
		oldRecipe.putAll(getRecipe());
		recipe.remove(ingredient);
		this.notifyUpdate("Recipe", oldRecipe.toString(), this.recipe.toString());
	}
	
	public void setAvailability(boolean availability) {
		this.availability = availability;
	}
	
	public boolean getAvailability() {
		return this.availability;
	}
	
	public synchronized void setRestockStatus(boolean status) {
		beingRestocked = status;
	}
	
	public boolean beingRestocked() {
		return beingRestocked;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.notifyUpdate("Name", this.name, name);
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.notifyUpdate("Description", this.description, description);
		this.description = description;
	}

	public Number getPrice() {
		return price;
	}

	public void setPrice(Number price) {
		this.notifyUpdate("Price", this.price, price);
		this.price = price;
	}

	public Map <Ingredient,Number> getRecipe() {
		return recipe;
	}

	public void setRecipe(Map <Ingredient,Number> recipe) {
		this.notifyUpdate("Recipe", this.recipe.toString(), recipe.toString());
		this.recipe = recipe;
	}

	public void setRestockThreshold(Number restockThreshold) {
		this.notifyUpdate("Restock Threshold", this.restockThreshold, restockThreshold);
		this.restockThreshold = restockThreshold;
	}
	
	public void setRestockAmount(Number restockAmount) {
		this.notifyUpdate("Restock Amount", this.restockAmount, restockAmount);
		this.restockAmount = restockAmount;
	}

	public Number getRestockThreshold() {
		return this.restockThreshold;
	}

	public Number getRestockAmount() {
		return this.restockAmount;
	}	
	
	public boolean equals(Object o) {
	    if (o == this) {
	      return true;
	    }
	    if (!(o instanceof Dish)) {
	      return false;
	    }
	    Dish orderInstance = (Dish)o;
	    return orderInstance.name.equals(name) &&
	    		orderInstance.description.equals(description);
	}
	 
	  public int hashCode() {
	    int result = 17;
	    result = 31 * result + name.hashCode();
	    result = 31 * result + description.hashCode();
	    return result;
	}
}
