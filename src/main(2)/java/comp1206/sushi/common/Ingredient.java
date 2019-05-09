package comp1206.sushi.common;

import java.io.Serializable;

import comp1206.sushi.common.Ingredient;
import comp1206.sushi.common.Supplier;

public class Ingredient extends Model implements Serializable{

	private static final long serialVersionUID = -2146403233795769902L;
	private volatile boolean availability;
	private volatile boolean beingRestocked;
	private String name;
	private String unit;
	private Supplier supplier;
	private Number restockThreshold;
	private Number restockAmount;
	private Number weight;
	private volatile String restockType;
	
	public Ingredient(String name, String unit, Supplier supplier, Number restockThreshold,
			Number restockAmount, Number weight) {
		this.setName(name);
		this.setUnit(unit);
		this.setSupplier(supplier);
		this.setRestockThreshold(restockThreshold);
		this.setRestockAmount(restockAmount);
		this.setWeight(weight);
		this.setRestockStatus(false);
		this.restockType = "Normal";
	}
	
	public String getRestockType() {
		return restockType;
	}
	
	public void setRestockType(String type) {
		this.restockType = type;
	}
	
	public void setRestockStatus(boolean status) {
		this.beingRestocked = status;
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

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.notifyUpdate("Ingredient Unit", this.unit, unit);
		this.unit = unit;
	}

	public Supplier getSupplier() {
		return supplier;
	}

	public void setSupplier(Supplier supplier) {
		this.notifyUpdate("Supplier", this.supplier, supplier);
		this.supplier = supplier;
	}

	public Number getRestockThreshold() {
		return restockThreshold;
	}

	public void setRestockThreshold(Number restockThreshold) {
		this.notifyUpdate("Restock Threshold", this.restockThreshold, restockThreshold);
		this.restockThreshold = restockThreshold;
	}

	public Number getRestockAmount() {
		return restockAmount;
	}

	public void setRestockAmount(Number restockAmount) {
		this.notifyUpdate("Restock Amount", this.restockAmount, restockAmount);
		this.restockAmount = restockAmount;
	}

	public Number getWeight() {
		return weight;
	}

	public void setWeight(Number weight) {
		this.notifyUpdate("Weight", this.weight, weight);
		this.weight = weight;
	}
	
	public void setIngredientAvailability(boolean availability) {
		this.availability = availability;
	}
	
	public boolean getIngredientAvailability() {
		return availability;
	}
}
