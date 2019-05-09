package comp1206.sushi.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import comp1206.sushi.common.Staff;
import comp1206.sushi.common.Supplier;
import comp1206.sushi.common.User;

public class Configuration {
	
	private Restaurant restaurant;
	private String filename;
	private Server server;
	private Postcode restaurantPostcode;
	private String restaurantName;
	private ArrayList<User> users;
	private List<Staff> staffs;
	private CopyOnWriteArrayList<Dish> dishes;
	private CopyOnWriteArrayList<Ingredient> ingredients;
	private ArrayList<Drone> drones;
	private List<Order> orders;
	private DishStockManager dishStockManager;
	private IngredientStockManager ingredientStockManager;
	private Map<Dish, Number> configDishStock;
	private Map<Ingredient, Number> configIngredientStock;
	private DataPersistence dp;
	private List<String> staffData = Collections.synchronizedList(new ArrayList<>());
	private List<Integer> droneData = Collections.synchronizedList(new ArrayList<>());
	
	public Configuration(String filename, Server server) {
		this.filename=filename;
		this.server = server;	
		this.server.clearAllData();
		this.users = new ArrayList<>();
		this.dishes = new CopyOnWriteArrayList<>();
		this.ingredients = new CopyOnWriteArrayList<>();
		this.orders = server.getOrders();
		this.drones = new ArrayList<>();
		this.configDishStock = new ConcurrentHashMap<>();
		this.configIngredientStock = new ConcurrentHashMap<>();
		this.ingredientStockManager = new IngredientStockManager();
		this.dishStockManager = new DishStockManager();
		this.dp = server.getDP();
		this.staffs = new ArrayList<>();
		
		try {
			readDataFromConfig();
		}
		catch(FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		}
	}
	
	public void readDataFromConfig() throws FileNotFoundException {
		BufferedReader bf = new BufferedReader(new FileReader(filename));
		String line;
	
		try {
			while((line = bf.readLine()) != null) {
				String[] data = line.split(":");
				
				if (line.contains("POSTCODE")) {
					Postcode newPostcode = new Postcode(data[1]);
					if (!server.getPostcodes().contains(newPostcode)) {
						server.addPostcode(data[1]);
					}
				}
				
				else if (line.contains("RESTAURANT")) {
					restaurantName = data[1];
					restaurantPostcode = null;
					for (Postcode currentPostcode: server.getPostcodes()) {
						if (currentPostcode.getName().equals(data[2])) {
							restaurantPostcode = currentPostcode;
							currentPostcode.setDistanceZero();
							restaurant = new Restaurant(restaurantName, restaurantPostcode);
							server.setRestaurantFromConfig(restaurant);
							dp.writeRestaurant(restaurant);
							break;
						}
					}
					
					if (restaurantPostcode == null) {
						Postcode newPostcode = new Postcode(data[2]);
						restaurantPostcode = newPostcode;
						restaurant = new Restaurant(restaurantName, restaurantPostcode);
						server.setRestaurantFromConfig(restaurant);
						server.addPostcode(data[2]);
						dp.writeRestaurant(restaurant);
					}
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
							if (valdateInt(data[4]) && valdateInt(data[5]) && valdateInt(data[6])) {
								int restockThreshold = Integer.parseInt(data[4]);
								int restockAmount = Integer.parseInt(data[5]);
								int weight = Integer.parseInt(data[6]);
								Ingredient ingredient = new Ingredient (data[1], data[2], supplier, restockThreshold, restockAmount, weight);
								ingredients.add(ingredient);
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
					dishToAdd.addUpdateListener(server);
					for (String current: recipe) {
						String[] ingredientQttyPair = current.split(" \\* ");
						int quantity = Integer.parseInt(ingredientQttyPair[0]);
						
						for (Ingredient currentIngredient: this.ingredients) {
							if (currentIngredient.getName().equals(ingredientQttyPair[1])) {
								dishRecipe.put(currentIngredient, quantity);
								break;
							}
						}
					}
					dishToAdd.setRecipe(dishRecipe);
					dishes.add(dishToAdd);
					configDishStock.put(dishToAdd, (int)0);
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
							if (valdateInt(data[2])) {
								int dishStock = Integer.parseInt(data[2]);
								configDishStock.put(dish, dishStock);
								break;
							}
						}
					}
					
					for (Ingredient ingredient: this.ingredients) {
						if (ingredient.getName().equals(data[1])) {
							if (valdateInt(data[2])) {
								int ingredientStock = Integer.parseInt(data[2]);
								configIngredientStock.put(ingredient, (int)ingredientStock);
								break;
							}
						}
					}
				}
				
				else if (line.contains("STAFF")) {
					Staff newStaff = new Staff(data[1], server);
					staffs.add(newStaff);
					staffData.add(data[1]);
				}
				
				else if (line.contains("DRONE")) {
					if (validateDroneSpeed(data[1]) == true) {
						int droneSpeed = Integer.parseInt(data[1]);
						Drone newDrone = new Drone(droneSpeed);
						drones.add(newDrone);
						droneData.add(droneSpeed);
					}
				}
			}
			
			ingredientStockManager.initializeIngredientStock(configIngredientStock);
			dishStockManager.initializeDishStock(configDishStock);
			server.setIngredientManager(ingredientStockManager);
			server.setDishManager(dishStockManager);
			dp.writeDishManager(dishStockManager);
			dp.writeIngredientManager(ingredientStockManager);
			dp.writeOrders(orders);
			dp.writeDrones(droneData);
			dp.writeStaffs(staffData);
			server.setStaffsFromConfig(staffs);
			server.setDroneData(droneData);
			server.setStaffData(staffData);
			server.setDronesFromConfig(drones);
			server.setOrders();
			bf.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	public boolean valdateInt(String value) {
		if (StringUtils.isNumeric(value)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean validateDroneSpeed(String value) {
		try {
			Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
	}
}
