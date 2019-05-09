package comp1206.sushi.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;

import comp1206.sushi.common.Basket;
import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Drone;
import comp1206.sushi.common.Ingredient;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Postcode;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.Supplier;
import comp1206.sushi.common.User;

public class Configuration {
	
	private String filename;
	private Server server;
	private Postcode restaurantPostcode;
	private String restaurantName;
	private ArrayList<User> users;
	private CopyOnWriteArrayList<Dish> dishes;
	private ArrayList<Drone> drones;
	private ArrayList<Order> orders;
	private DishStockManager dishStockManager;
	private IngredientStockManager ingredientStockManager;
	private Map<Dish, Number> configDishStock;
	private Map<Ingredient, Number> configIngredientStock;
	
	public Configuration(String filename, Server server, DishStockManager serverStockManager, IngredientStockManager serverIngredientManager) {
		this.filename=filename;
		this.server = server;	
		this.users = new ArrayList<>();
		this.dishes = new CopyOnWriteArrayList<>();
		this.orders = new ArrayList<>();
		this.drones = new ArrayList<>();
		this.configDishStock = new ConcurrentHashMap<>();
		this.configIngredientStock = new ConcurrentHashMap<>();
		this.ingredientStockManager = serverIngredientManager;
		this.dishStockManager = serverStockManager;
		removeServerData();
		
		try {
			readDataFromConfig();
		}
		catch(FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		}
	}
	
	public void removeServerData() {
		server.getDishes().clear();
		server.getDrones().clear();
		server.getStaff().clear();
		server.getIngredients().clear();
		server.getSuppliers().clear();
		server.getUsers().clear();
		server.getOrders().clear();
		server.getPostcodes().clear();
	}
	
	public void readDataFromConfig() throws FileNotFoundException {
		BufferedReader bf = new BufferedReader(new FileReader(filename));
		String line;
	
		try {
			while((line = bf.readLine()) != null) {
				String[] data = line.split(":");
				
				if (line.contains("POSTCODE")) {
					server.addPostcode(data[1]);
				}
				
				else if (line.contains("RESTAURANT")) {
					restaurantName = data[1];
					restaurantPostcode = null;
					for (Postcode currentPostcode: server.getPostcodes()) {
						if (currentPostcode.getName().equals(data[2])) {
							restaurantPostcode = currentPostcode;
							break;
						}
					}
					
					if (restaurantPostcode == null) {
						Postcode newPostcode = server.addPostcode(data[2]);
						restaurantPostcode = newPostcode;
					}
					
					Restaurant restaurant = new Restaurant(restaurantName, restaurantPostcode);
					server.setRestaurantFromConfig(restaurant);
				}
				
				else if (line.contains("SUPPLIER")) {
					for (Postcode postcode: server.getPostcodes()) {
						if (postcode.getName().equals(data[2])) {			
							server.addSupplier(data[1], postcode);
							break;
						}
					}
				}
				
				else if (line.contains("INGREDIENT")) {
					for (Supplier supplier: server.getSuppliers()) {
						if (supplier.getName().equals(data[3])) {
							if (validateNumeric(data[4]) && validateNumeric(data[5]) && validateNumeric(data[6])) {
								int restockThreshold = Integer.parseInt(data[4]);
								int restockAmount = Integer.parseInt(data[5]);
								int weight = Integer.parseInt(data[6]);
								Ingredient ingredient = server.addIngredient(data[1], data[2], supplier, restockThreshold, restockAmount, weight);
								configIngredientStock.put(ingredient, (int)0);
								break;
							}
						}
					}
				}
				
				else if (line.contains("DISH")) {
					int price = Integer.parseInt(data[3]);
					int restockThreshold = Integer.parseInt(data[4]);
					int restockAmount = Integer.parseInt(data[5]);
					String[] recipe = data[6].split(",");
					Map<Ingredient, Number> dishRecipe = new HashMap<>();
					Dish dishToAdd = new Dish(data[1], data[2], price, restockThreshold, restockAmount);
					dishes.add(dishToAdd);
					configDishStock.put(dishToAdd, (int)0);
					for (String current: recipe) {
						String[] ingredientQttyPair = current.split(" \\* ");
						int quantity = Integer.parseInt(ingredientQttyPair[0]);
						
						for (Ingredient currentIngredient: server.getIngredients()) {
							if (currentIngredient.getName().equals(ingredientQttyPair[1])) {
								dishRecipe.put(currentIngredient, quantity);
								break;
							}
						}
					}
					dishToAdd.setRecipe(dishRecipe);
				}
				
				else if (line.contains("USER")) {
					Postcode userPostcode = null;
					for (Postcode current: server.getPostcodes()) {
						if (current.getName().equals(data[4])) {
							userPostcode = current;
							break;
						}
					}
					
					if (userPostcode == null) {
						Postcode newPostcode = server.addPostcode(data[4]);
						userPostcode = newPostcode;
					}
					
					User userFromConfig = new User(data[1], data[2], data[3], userPostcode);
					users.add(userFromConfig);
					server.addUser(userFromConfig);
				}
				
				else if (line.contains("ORDER")) {
					for (User current: users) {
						if (current.getName().equals(data[1])) {
							Basket userBasket = current.getBasket();
							String[] orders = data[2].split(",");
							for (String currentOrder: orders) {
								String[] orderQuantityPair = currentOrder.split(" \\* ");
								for (Dish currentDish: dishes) {
									if (currentDish.getName().equals(orderQuantityPair[1])) {
										int quantity = Integer.parseInt(orderQuantityPair[0]);
										userBasket.addDishToBasket(currentDish, quantity);
										break;
									}
								}
							}
							Order currentOrder = new Order(current);
							current.addNewOrder(currentOrder);
							this.orders.add(currentOrder);
						}
					}
				}
				
				else if (line.contains("STOCK")) {
					for (Dish dish: dishes) {
						if (dish.getName().equals(data[1])) {
							if (validateNumeric(data[2])) {
								int dishStock = Integer.parseInt(data[2]);
								configDishStock.put(dish, dishStock);
								break;
							}
						}
					}
					
					for (Ingredient ingredient: server.getIngredients()) {
						if (ingredient.getName().equals(data[1])) {
							if (validateNumeric(data[2])) {
								int ingredientStock = Integer.parseInt(data[2]);
								configIngredientStock.put(ingredient, (int)ingredientStock);
								break;
							}
						}
					}
				}
				
				else if (line.contains("STAFF")) {
					server.addStaff(data[1]);
				}
				
				else if (line.contains("DRONE")) {
					if (validateNumeric(data[1]) == true) {
						float droneSpeed = Float.parseFloat(data[1]);
						drones.add(new Drone(droneSpeed));
					}
				}
			}
			
			ingredientStockManager.initializeStockFromConfig(configIngredientStock);
			dishStockManager.initializeStockFromConfig(configDishStock);
			server.setDishesFromConfig(dishes);
			server.setDronesFromConfig(drones);
			server.setOrders(this.orders);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	public boolean validateNumeric(String value) {
		if (StringUtils.isNumeric(value)) {
			return true;
		}
		else {
			return false;
		}
	}
}
