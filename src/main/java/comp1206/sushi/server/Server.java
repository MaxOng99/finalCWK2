package comp1206.sushi.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Drone;
import comp1206.sushi.common.Ingredient;
import comp1206.sushi.common.Model;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Postcode;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.Staff;
import comp1206.sushi.common.Supplier;
import comp1206.sushi.common.UpdateEvent;
import comp1206.sushi.common.UpdateListener;
import comp1206.sushi.common.User;
 
public class Server implements ServerInterface, UpdateListener{

	private static final Logger logger = LogManager.getLogger("Server");
	
	private List<ServerMailBox> mailBoxes = Collections.synchronizedList(new ArrayList<ServerMailBox>());
	private List<Order> orders = Collections.synchronizedList(new ArrayList<Order>());
	private List<User> users = Collections.synchronizedList(new ArrayList<User>());
	private List<Drone> drones = new ArrayList<Drone>();
	private List<Staff> staff = new ArrayList<Staff>();
	private List<Supplier> suppliers = new ArrayList<Supplier>();
	private List<Postcode> postcodes = new ArrayList<Postcode>();
	private List<String> staffData = Collections.synchronizedList(new ArrayList<>());
	private List<Integer> droneData = Collections.synchronizedList(new ArrayList<>());
	private List<UpdateListener> listeners = new ArrayList<UpdateListener>();
	private BlockingQueue<Order> orderDeliveryQueue = new LinkedBlockingQueue<>();
	private BlockingQueue<Order> waitingForDishRestock = new LinkedBlockingQueue<>();
	private Restaurant restaurant = new Restaurant("Southampton Sushi", new Postcode("SO17 1BJ"));
	private IngredientStockManager ingredientManager = new IngredientStockManager();
	private DishStockManager dishManager = new DishStockManager();
	private Object lockOrder = new Object();
	private Object dishLock = new Object();
	private Object userListLock = new Object();
	private DataPersistence dp;
	private Thread checkAbleToDeliverThread;
	private Thread clientListenerThread;
	private boolean reloadConfig;
	
	public Server() {
		clearAllData();
		logger.info("Starting up server...");
		dp = new DataPersistence(this);
		
		if (dp.persistentFilesExist()) {
			dp.readUsers();
			dp.readStaff();
			dp.readDrones();
			dp.readOrders();
		}
		clientListenerThread = new Thread(new ClientListener(this, logger));
		clientListenerThread.start();
	}
	
	@Override
	public void loadConfiguration(String filename) {
		clearAllData();
		logger.info("Loading configuration from " + filename);
		new Configuration(filename, this);	
		logger.info("Configuration successfuly loaded");
		
		synchronized(mailBoxes) {
			Iterator<ServerMailBox> serverMailBoxIt = mailBoxes.iterator();
			while (serverMailBoxIt.hasNext()) {
				ServerMailBox nextMailBox = serverMailBoxIt.next();
				try {
					User user = nextMailBox.getRegisteredUser();
					
					if (user != null) {
						this.addUser(user);
					}
					nextMailBox.sendInitialDataToClient();
				}
				catch(IOException e) {
					logger.info("Lost connection to client /127.0.0.1");
				}
			}
		}
	}
	
	public void setDishManager (DishStockManager dishManager) {
		this.dishManager = dishManager;
		dishManager.initialRestockProcess();
	}
	
	public DishStockManager getDishManager() {
		return dishManager;
	}
	
	public void setIngredientManager(IngredientStockManager ingredientManager) {
		this.ingredientManager = ingredientManager;
		this.ingredientManager.initialRestockProcess();	
	}
	
	public IngredientStockManager getIngredientManager() {
		return ingredientManager;
	}
	
	public void setDP(DataPersistence dp) {
		this.dp = dp;
	}
	
	public DataPersistence getDP() {
		return dp;
	}
	
	public void setUsers(List<User> savedUsers) {
		this.users = savedUsers;
	}
	
	@Override
	public List<User> getUsers() {
		synchronized(userListLock) {
			return this.users;
		}
	}
	
	public void setPostcodes(List<Postcode> savedPostcode) {
		this.postcodes = savedPostcode;
	}
	
	@Override
	public List<Postcode> getPostcodes() {
		return this.postcodes;
	}
	
	public void setRestaurantFromConfig(Restaurant restaurantFromConfig) {
		this.restaurant = restaurantFromConfig;
	}
	
	@Override
	public String getRestaurantName() {
		return restaurant.getName();
	}

	@Override
	public Postcode getRestaurantPostcode() {
		return restaurant.getLocation();
	}
	
	@Override
	public Restaurant getRestaurant() {
		return restaurant;
	}
	
	public void setStaffsFromConfig(List<Staff> staffsFromConfig) {
		this.staff = staffsFromConfig;
		for (Staff currentStaff: staff) {
			currentStaff.setDishStckManager(dishManager);
			currentStaff.addUpdateListener(this);
		}
	}
	
	public void setStaffData(List<String>  staffData) {
		this.staffData = staffData;
	}
	
	@Override
	public List<Staff> getStaff() {
		return this.staff;
	}
	
	public void setDronesFromConfig(List<Drone> dronesFromConfig) {
		this.drones = dronesFromConfig;
		for (Drone drone: drones) {
			drone.addUpdateListener(this);
			drone.setIngredientStockManager(ingredientManager);
			drone.setOrderQueue(orderDeliveryQueue);
			drone.setServer(this);
			drone.setRestaurantPostcode(getRestaurantPostcode());
		}
	}
	
	public void setDroneData(List<Integer> droneData) {
		this.droneData = droneData;
	}
	
	@Override
	public List<Drone> getDrones() {
		return this.drones;
	}
	
	public void setSuppliers(List<Supplier> savedSuppliers) {
		this.suppliers = savedSuppliers;
	}
	
	@Override
	public List<Supplier> getSuppliers() {
		return this.suppliers;
	}
	
	public void setSavedOrders(List<Order> savedOrders) {
		this.orders = savedOrders;
		setOrders();
	}
	
	public void setOrders() {
		reloadConfig = false;
		checkAbleToDeliverThread = new Thread(new CheckAbleToDeliver());
		checkAbleToDeliverThread.start();
		
		synchronized(orders) {
			Iterator<Order> orderIt = orders.iterator();
			while (orderIt.hasNext()) {
				Order nextOrder = orderIt.next();
				Map<Dish, Number> dishesOrdered = nextOrder.getUser().getBasket().getBasketMap();
				
				if (nextOrder.getStatus().equals("Order being delivered..."))  {
					try {
						orderDeliveryQueue.put(nextOrder);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				else {
					if (ableToDeliverOrder(nextOrder)) {
						try {
							nextOrder.setStatus("Order being delivered...");
							orderDeliveryQueue.put(nextOrder);
							for (Entry<Dish, Number> currentOrder: dishesOrdered.entrySet()) {
								String dishOrderedName = currentOrder.getKey().getName();
								int currentDishStockDeducted = (int)currentOrder.getValue();
								Iterator<Dish> dishIt = getDishes().iterator();
								while(dishIt.hasNext()) {
									Dish nextDish = dishIt.next();
									if (nextDish.getName().equals(dishOrderedName)) {
										setStock(nextDish, -currentDishStockDeducted);
									}
								}
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					else {
						try {
							waitingForDishRestock.put(nextOrder);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			dp.writeIngredientManager(ingredientManager);
		}
	}
	
	@Override
	public List<Dish> getDishes() {
		return dishManager.getDishes();
	}
	
	public void addMailBoxes(ServerMailBox mailBox) {
		mailBoxes.add(mailBox);
	}
	
	public void removeMailBoxes(ServerMailBox mailBox) {
		mailBoxes.remove(mailBox);
	}
	
	public void informClientsOfDishUpdates(Dish dish) {
		synchronized(mailBoxes) {
			Iterator<ServerMailBox> mailBoxIt = mailBoxes.iterator();
			while (mailBoxIt.hasNext()) {
				ServerMailBox nextMailBox = mailBoxIt.next();
				try {
					nextMailBox.sendNewDish(dish);
				}
				catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public Dish addDish(String name, String description, Number price, Number restockThreshold, Number restockAmount) {
		Dish newDish = new Dish(name,description,price,restockThreshold,restockAmount);
		newDish.addUpdateListener(this);
		newDish.setAvailability(true);
		this.informClientsOfDishUpdates(newDish);
		this.notifyUpdate();
		return newDish;
	}
	
	@Override
	public void removeDish(Dish dish) throws UnableToDeleteException{
		 
		if (dish.beingRestocked()) {
			throw new UnableToDeleteException(dish.getName() + " is currently being restocked");
		}
		else if (dishBeingOrdered(dish)) {
			throw new UnableToDeleteException(dish.getName() + " is in an order that is incomplete");
		}
		else {
			dishManager.removeDish(dish);
			dp.writeDishManager(dishManager);
			dish.setAvailability(false);
			this.informClientsOfDishUpdates(dish);
			this.notifyUpdate();
			logDeletedModel(dish);
		}
		
	}

	public boolean dishBeingOrdered(Dish dish) {
		boolean beingOrdered = false;
		for (Order order: this.orders) {
			String orderStatus = order.getStatus();
			if (orderStatus.equals("Incomplete") || orderStatus.equals("Order being delivered...")) {
				Map<Dish, Number> orderedDishes = order.getUser().getBasket().getBasketMap();
				for (Dish orderedDish: orderedDishes.keySet()) {
					if(orderedDish.getName().equals(dish.getName())) {
						beingOrdered = true;
						return beingOrdered;
					}
				}
			}
		}
		return beingOrdered;
	}
	
	@Override
	public Map<Dish, Number> getDishStockLevels() {
		return dishManager.getDishStockLevels();
	}
	
	@Override
	public void setRestockingIngredientsEnabled(boolean enabled) {
		for (Drone drone: this.drones) {
			drone.setRestockStatus(false);
		}
	}

	@Override
	public void setRestockingDishesEnabled(boolean enabled) {
		for (Staff staff: this.staff) {
			staff.setRestockStatus(false);
		}
	}
	
	@Override
	public void setStock(Dish dish, Number stock) {
		synchronized(dishLock) {
			dishManager.setStock(dish, stock);
			this.notifyUpdate();
		}
	}

	@Override
	public void setStock(Ingredient ingredient, Number stock) {
		ingredientManager.setStock(ingredient, stock);
		this.notifyUpdate();
	}

	@Override
	public List<Ingredient> getIngredients() {
		return ingredientManager.getIngredients();
	}

	@Override
	public Ingredient addIngredient(String name, String unit, Supplier supplier,
		Number restockThreshold, Number restockAmount, Number weight) {
		Ingredient mockIngredient = new Ingredient(name,unit,supplier,restockThreshold,restockAmount,weight);
		mockIngredient.addUpdateListener(this);
		ingredientManager.addIngredient(mockIngredient, 0);
		this.notifyUpdate();
		dp.writeIngredientManager(ingredientManager);
		return mockIngredient;
	}

	@Override
	public void removeIngredient(Ingredient ingredient) throws UnableToDeleteException {
		int index = ingredientManager.getIngredients().indexOf(ingredient);
		if(ingredient.beingRestocked()) {
			throw new UnableToDeleteException(ingredient.getName() + " is currently being restocked");
		}
		else if (checkIfIngredientInUse(ingredient)) {
			throw new UnableToDeleteException(ingredient.getName() + " is still used");
		}
		else {
			ingredientManager.getIngredients().remove(index);
			ingredientManager.removeIngredientFromStockManager(ingredient);
			dp.writeIngredientManager(ingredientManager);
			this.notifyUpdate();
			logDeletedModel(ingredient);
		}
	}

	

	@Override
	public Supplier addSupplier(String name, Postcode postcode) {
		Supplier mock = new Supplier(name,postcode);
		this.suppliers.add(mock);
		this.notifyUpdate();
		dp.writeSuppliers(this.suppliers);
		return mock;
	}


	@Override
	public void removeSupplier(Supplier supplier) throws UnableToDeleteException{
		if (checkIfSupplierInUse(supplier)) {
			throw new UnableToDeleteException(supplier.getName() + " is still supplying ingredients");
		}
		else {
			int index = this.suppliers.indexOf(supplier);
			this.suppliers.remove(index);
			this.notifyUpdate();
			dp.writeSuppliers(suppliers);
			logDeletedModel(supplier);
		}
		
	}

	

	@Override
	public Drone addDrone(Number speed) {
		Drone mock = new Drone(speed);
		mock.addUpdateListener(this);
		mock.setIngredientStockManager(ingredientManager);
		mock.setOrderQueue(orderDeliveryQueue);
		mock.setRestaurantPostcode(getRestaurantPostcode());
		mock.setServer(this);
		this.drones.add(mock);
		this.droneData.add((Integer)speed);
		this.notifyUpdate();
		dp.writeDrones(droneData);
		return mock;
	}

	@Override
	public void removeDrone(Drone drone) throws UnableToDeleteException{
		if(checkIfDroneInUse(drone)) {
			throw new UnableToDeleteException(drone.getName() + " is still in use");
		}
		else {
			for (Integer droneSpeed: droneData) {
				if (droneSpeed == drone.getSpeed()) {
					droneData.remove(droneSpeed);
					break;
				}
			}
			drone.deleteFromServer();
			int index = this.drones.indexOf(drone);
			this.drones.remove(index);
			dp.writeDrones(droneData);
			this.notifyUpdate();
			logDeletedModel(drone);
		}
	}

	

	@Override
	public Staff addStaff(String name) {
		Staff mock = new Staff(name, this);
		mock.setStatus("Idle");
		mock.setDishStckManager(dishManager);
		mock.addUpdateListener(this);
		this.staff.add(mock);
		this.staffData.add(name);
		this.notifyUpdate();
		dp.writeStaffs(staffData);
		return mock;
	}
	
	public boolean ableToDeliverOrder(Order order) {
		
		if (order.getStatus().equals("Complete")) {
			return false;
		}
		
		else {
			Map<Dish, Number> dishesOrdered = order.getUser().getBasket().getBasketMap();
			for (Entry<Dish, Number> currentEntry: dishesOrdered.entrySet()) {		
				Dish currentDish = currentEntry.getKey();
				int currentDishStock = dishManager.getStock(currentDish);
				
				if (currentDishStock < currentEntry.getValue().intValue()) {
					if (currentDishStock < (int)currentDish.getRestockThreshold()) {
						return false;
					}
				}
			}
			return true;
		}
	}
	
	public void addOrder(Order order) {
		
		synchronized(lockOrder) {	
			order.addUpdateListener(this);
			orders.add(order);
			this.notifyUpdate();
			Map<Dish, Number> dishesOrdered = order.getUser().getBasket().getBasketMap();
			if (ableToDeliverOrder(order)) {
				try {
					order.setStatus("Order being delivered...");
					orderDeliveryQueue.put(order);
					for (Entry<Dish, Number> currentOrder: dishesOrdered.entrySet()) {
						Dish currentDish = currentOrder.getKey();
						int currentDishStockDeducted = (int)currentOrder.getValue();
						setStock(currentDish, -currentDishStockDeducted);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else {
				try {
					waitingForDishRestock.put(order);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			dp.writeOrders(orders);
			dp.writeDishManager(dishManager);
		}
	}
	@Override
	public void removeStaff(Staff staff) throws UnableToDeleteException{
		if (checkIfStaffIsWorking(staff)) {
			throw new UnableToDeleteException(staff.getName() + " is still working");
		}
		else {
			this.staff.remove(staff);
			staff.deleteFromServer();
			this.notifyUpdate();
			for (String staffName: staffData) {
				if (staffName.equals(staff.getName())) {
					staffData.remove(staffName);
					break;
				}
			}
			
			dp.writeStaffs(staffData);
			logDeletedModel(staff);
		}
	}

	@Override
	public List<Order> getOrders() {
		return this.orders;
	}

	@Override
	public void removeOrder(Order order) throws UnableToDeleteException{
		if (ableToDeleteOrder(order)) {
			int index = this.orders.indexOf(order);
			this.orders.remove(index);
			this.notifyUpdate();
			dp.writeOrders(orders);
			logDeletedModel(order);
		}
		else {
			throw new UnableToDeleteException(order.getName() + " is not complete yet");
		}
	}
	
	@Override
	public Number getOrderCost(Order order) {
		return order.getCost();
	}

	@Override
	public Map<Ingredient, Number> getIngredientStockLevels() {
		return ingredientManager.getIngredientStockLevel();
	}

	@Override
	public Number getSupplierDistance(Supplier supplier) {
		return supplier.getDistance();
	}

	@Override
	public Number getDroneSpeed(Drone drone) {
		return drone.getSpeed();
	}

	@Override
	public Number getOrderDistance(Order order) {
		return order.getDistance();
	}

	@Override
	public void addIngredientToDish(Dish dish, Ingredient ingredient, Number quantity) {
		dish.addIngredientToRecipe(ingredient, quantity);
		this.notifyUpdate();
		dp.writeDishManager(dishManager);
	}

	@Override
	public void removeIngredientFromDish(Dish dish, Ingredient ingredient) {
		dish.removeIngredientFromRecipe(ingredient);
		this.notifyUpdate();
		dp.writeDishManager(dishManager);
	}

	@Override
	public Map<Ingredient, Number> getRecipe(Dish dish) {
		return dish.getRecipe();
	}
	
	@Override
	public Postcode addPostcode(String code) {
		Postcode mock = new Postcode(code);
		
		if (restaurant!= null) {
			if (restaurant.getLocation().equals(mock)) {
				mock.setDistanceZero();
			}
			
			else {
				mock.calculateDistance(restaurant);
			}
		}
		
		mock.addUpdateListener(this);
		this.postcodes.add(mock);
		this.notifyUpdate();
		dp.writePostcodes(this.getPostcodes());
		return mock;
	}

	@Override
	public void removePostcode(Postcode postcode) throws UnableToDeleteException {
		if(checkIfPostcodeInUse(postcode)) {
			throw new UnableToDeleteException(postcode.getName() + " is still used by suppliers");
		}
		else {
			this.postcodes.remove(postcode);
			this.notifyUpdate();
			dp.writePostcodes(getPostcodes());
		}
	}
	
	public void addUser(User user) {
		synchronized(userListLock) {
			users.add(user); 
			dp.writeUsers(this.getUsers());
		}
	}
	
	
	
	@Override
	public void removeUser(User user) throws UnableToDeleteException{
		synchronized(userListLock) {
			
			if(!(ableToDeleteUser(user))) {
				throw new UnableToDeleteException(user.getName() + " still have outstanding orders");
			}
			
			else {
				this.users.remove(user);
				this.notifyUpdate();
				dp.writeUsers(users);
			}
		}
	}
	
	public boolean ableToDeleteUser(User user) {
		boolean ableToDeleteUser = true;
		for (Order order: user.getOrders()) {
			if(!(ableToDeleteOrder(order))) {
				ableToDeleteUser = false;
				break;
			}
		}
		return ableToDeleteUser;
	}
	@Override
	public void setRecipe(Dish dish, Map<Ingredient, Number> recipe) {
		dish.setRecipe(recipe);
		this.notifyUpdate();
	}
	
	@Override
	public boolean isOrderComplete(Order order) {
		if (order.getStatus().equals("Complete")) {
			return true;
		}
		
		else {
			return false;
		}
	}

	@Override
	public String getOrderStatus(Order order) {
		return order.getStatus();
	}
	
	@Override
	public String getDroneStatus(Drone drone) {
		return drone.getStatus();
	}
	
	@Override
	public String getStaffStatus(Staff staff) {
		return staff.getStatus();
	}

	@Override
	public void setRestockLevels(Dish dish, Number restockThreshold, Number restockAmount) {
		dish.setRestockThreshold(restockThreshold);
		dish.setRestockAmount(restockAmount);
		
		if (!dishManager.getDishes().contains(dish)) {
			dishManager.addDish(dish, 0);
			dp.writeDishManager(dishManager);
			dp.writeIngredientManager(ingredientManager);
		}
		
		this.notifyUpdate();
	}

	@Override
	public void setRestockLevels(Ingredient ingredient, Number restockThreshold, Number restockAmount) {
		ingredient.setRestockThreshold(restockThreshold);
		ingredient.setRestockAmount(restockAmount);
		this.notifyUpdate();
	}

	@Override
	public Number getRestockThreshold(Dish dish) {
		return dish.getRestockThreshold();
	}

	@Override
	public Number getRestockAmount(Dish dish) {
		return dish.getRestockAmount();
	}

	@Override
	public Number getRestockThreshold(Ingredient ingredient) {
		return ingredient.getRestockThreshold();
	}

	@Override
	public Number getRestockAmount(Ingredient ingredient) {
		return ingredient.getRestockAmount();
	}

	@Override
	public void addUpdateListener(UpdateListener listener) {
		this.listeners.add(listener);
	}
	
	@Override
	public void notifyUpdate() {
		this.listeners.forEach(listener -> listener.updated(new UpdateEvent()));
	}

	@Override
	public Postcode getDroneSource(Drone drone) {
		return drone.getSource();
	}

	@Override
	public Postcode getDroneDestination(Drone drone) {
		return drone.getDestination();
	}

	@Override
	public Number getDroneProgress(Drone drone) {
		return drone.getProgress();
	}
	
	public boolean ableToDeleteOrder(Order order) {
		String orderStatus = order.getStatus();
		if (orderStatus.equals("Complete") || orderStatus.equals("Canceled")) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean checkIfDroneInUse(Drone drone) {
		boolean isUsed = false;
		if (!(drone.getStatus().equals("Idle"))) {
			isUsed = true;
		}
		return isUsed;
	}
	
	public boolean checkIfStaffIsWorking(Staff staff) {
		boolean isWorking = false;
		if (!(staff.getStatus().equals("Idle"))) {
			isWorking = true;
		}
		return isWorking;
	}
	
	public boolean checkIfIngredientInUse(Ingredient ingredient) {
		boolean isUsed = false;
		Iterator<Dish> serverDishIt = getDishes().iterator();
		while(serverDishIt.hasNext()) {
			Dish nextDish = serverDishIt.next();
			Map<Ingredient, Number> recipe = nextDish.getRecipe();
			
			for (Entry<Ingredient, Number> currentEntry: recipe.entrySet()) {
				if (currentEntry.getKey().getName().equals(ingredient.getName())) {
					isUsed=true;
					return isUsed;
				}
			}
		}
		return isUsed;
	}
	
	public boolean checkIfSupplierInUse(Supplier supplier) {
		boolean isUsed = false;
		Iterator<Ingredient> ingredientIt = ingredientManager.getIngredients().iterator();
		while(ingredientIt.hasNext()) {
			Ingredient nextIngredient = ingredientIt.next();
			if (nextIngredient.getSupplier().equals(supplier)) {
				isUsed = true;
				break;
			}
		}
		return isUsed;
	}
	
	public boolean checkIfPostcodeInUse(Postcode postcode) {
		boolean isUsed = false;
		Iterator<Supplier> supplierIt = this.suppliers.iterator();
		while(supplierIt.hasNext()) {
			Supplier nextSupplier = supplierIt.next();
			if (nextSupplier.getPostcode().equals(postcode)) {
				isUsed = true;
				break;
			}
		}
		return isUsed;
	}
	
	@Override
	public void updated(UpdateEvent updateEvent) {
		this.notifyUpdate();
		String modelName = updateEvent.model.getName();
		String updateProperty = updateEvent.property;
		Object oldValue = updateEvent.oldValue;
		Object newValue = updateEvent.newValue;
		
		if (oldValue != newValue) {
			logger.info(updateProperty+" of "+modelName+" has changed from "+oldValue+" to "+newValue);
		}
	}
	
	public void logDeletedModel(Model model) {
		logger.info(model + " has been deleted form the server");
	}
	
	public void clearAllData() {
		
		if (checkAbleToDeliverThread != null) {
			checkAbleToDeliverThread.interrupt();
			reloadConfig = true;
		}
		
		for (Drone drone: getDrones()) {
			drone.deleteFromServer();
			drone = null;
		}
		
		for (Staff staff: getStaff()) {
			staff.deleteFromServer();
			staff = null;
		}
		
		orders.clear();
		users.clear();
		orderDeliveryQueue.clear();
		waitingForDishRestock.clear();
		drones.clear();
		staff.clear();
		ingredientManager.clearAllData();
		dishManager.clearAllData();
		ingredientManager = new IngredientStockManager();
		dishManager = new DishStockManager();
		suppliers.clear();
		postcodes.clear();
		staffData.clear();
		droneData.clear();
		
		for (ServerMailBox serverMailBoxes: mailBoxes) {
			serverMailBoxes.terminateClient();
		}
		
		mailBoxes.clear();
	}

	class CheckAbleToDeliver implements Runnable{

		@Override
		public void run() {
			while(!reloadConfig) {
				try {
					Order order = waitingForDishRestock.take();
					if (ableToDeliverOrder(order)) {
						orderDeliveryQueue.put(order);
						order.setStatus("Order being delivered...");
						dp.writeOrders(orders);
						Map<Dish, Number> dishesOrdered = order.getUser().getBasket().getBasketMap();
						for (Entry<Dish, Number> currentOrder: dishesOrdered.entrySet()) {
							Dish currentDish = currentOrder.getKey();
							int currentDishStockDeducted = (int) currentOrder.getValue();
							System.out.println(currentDish+" "+currentDish.beingRestocked()+"-runnableAbleTo");
							setStock(currentDish, -currentDishStockDeducted);
						}
					}
					else {
						waitingForDishRestock.put(order);
					}
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
}
