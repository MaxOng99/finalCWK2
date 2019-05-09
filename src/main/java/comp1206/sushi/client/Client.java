package comp1206.sushi.client;

import java.io.IOException;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Postcode;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.UpdateEvent;
import comp1206.sushi.common.UpdateListener;
import comp1206.sushi.common.User;

public class Client implements ClientInterface {

    private static final Logger logger = LogManager.getLogger("Client");
 
    
    public Restaurant restaurant;
    public List<Order> orders;
	public List<Dish> dishes = new ArrayList<Dish>();
	public ArrayList<Postcode> postcodes = new ArrayList<Postcode>();
	private ArrayList<UpdateListener> listeners = new ArrayList<UpdateListener>();
	private ClientMailBox mailBox;
	private User registeredUser;
	
	public Client() {
		try {
			logger.info("Starting up client...");
			Postcode postcode1 = new Postcode("SO17 1TJ");
			Postcode postcode2 = new Postcode("SO17 1BX");
			Postcode postcode3 = new Postcode("SO17 2NJ");
			Postcode postcode4 = new Postcode("SO17 1TW");
			Postcode postcode5 = new Postcode("SO17 2LB");
			
			postcodes.add(postcode1);
			postcodes.add(postcode2);
			postcodes.add(postcode3);
			postcodes.add(postcode4);
			postcodes.add(postcode5);
			
			logger.info("Connecting to server...");
			connectToServer();
		}
		catch(Exception e) {
			logger.info("Terminating program...");
			System.exit(1);
		}
	}
	
	public void setRestaurantAndPostcodes(Restaurant restaurant) {
		this.restaurant = restaurant;
		
		for (Postcode postcode: postcodes) {
			postcode.calculateDistance(restaurant);
		}
	}
	
	public void setOrderList(List<Order> order) {
		this.orders = order;
	}
	public void connectToServer() throws Exception{
		logger.info("Connecting to server...");
		Socket clientSock = new Socket("127.0.0.1", 49920);
		logger.info("Connected to server");
		mailBox = new ClientMailBox(this, clientSock, logger);
		Thread msgManagerThread = new Thread(mailBox);
		msgManagerThread.start();
	}
		
	@Override
	public Restaurant getRestaurant() {
		return restaurant;
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
	public User register(String username, String password, String address, Postcode postcode) {
		
		boolean emptyUsername = username.trim().isEmpty();
		boolean emptyPassword = password.trim().isEmpty();
		boolean emptyAddress = address.trim().isEmpty();
		
		if (emptyUsername || emptyPassword || emptyAddress) {
			return null;
		}
		
		else {
			registeredUser = new User(username, password, address, postcode);
			try {
				mailBox.requestRegistration(registeredUser);
				mailBox.setRegisteredUser(registeredUser);
			} catch (IOException e) {
				e.printStackTrace();
			}
			setDishes(mailBox.getDishes());
			return registeredUser;
		}
	}

	@Override
	public User login(String username, String password) {
		String credential = username + ":" + password;
		try {
			mailBox.requestLogin(credential);
			Thread.sleep(3000);
		} 
		catch (IOException e1) {
			e1.printStackTrace();
		}
		catch(InterruptedException e2) {
			e2.printStackTrace();
		}
		setDishes(mailBox.getDishes());
		return getRegisteredUser();
	}

	@Override
	public List<Postcode> getPostcodes() {
		return postcodes;
	}
	
	public void setDishes(ArrayList<Dish> serverDishes) {
		dishes = serverDishes;
	}
	@Override
	public List<Dish> getDishes() {
		return dishes;
	}

	@Override
	public String getDishDescription(Dish dish) {
		return dish.getDescription();
	}

	@Override
	public Number getDishPrice(Dish dish) {
		return dish.getPrice();
	}

	@Override
	public Map<Dish, Number> getBasket(User user) {
		return user.getBasket().getBasketMap();
	}

	@Override
	public Number getBasketCost(User user) {
		return user.getBasket().getCost();
	}

	@Override
	public void addDishToBasket(User user, Dish dish, Number quantity) {
		user.getBasket().addDishToBasket(dish, quantity);
	}

	@Override
	public void updateDishInBasket(User user, Dish dish, Number quantity) {
		user.getBasket().updateDishInBasket(dish, quantity);
	}

	@Override
	public Order checkoutBasket(User user) {
		Order newOrder = new Order(user);
		newOrder.setStatus("Incomplete");
		user.addNewOrder(newOrder);
		try {
			mailBox.notifyNewOrder(newOrder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		clearBasket(user);
		return newOrder;
	}

	@Override
	public void clearBasket(User user) {
		user.getBasket().getBasketMap().clear();
		user.getBasket().setCost(0);
	}

	@Override
	public List<Order> getOrders(User user) {
		return user.getOrders();
	}

	@Override
	public boolean isOrderComplete(Order order) {
		if (order.getStatus().equals("Incomplete")) {
			return false;
		}
		else {
			return true;
		}
	}

	@Override
	public String getOrderStatus(Order order) {
		return order.getStatus();
	}

	@Override
	public Number getOrderCost(Order order) {
		return order.getCost();
	}

	@Override
	public void cancelOrder(Order order) {
		order.setStatus("Canceled");
		try {
			mailBox.requestOderCancelation(order);
		} catch (IOException e) {
			e.printStackTrace();
		}
		registeredUser.getOrders().remove(order);
		this.notifyUpdate();
		
	}
	
	public void addNewDish(Dish dish) {
		dishes.add(dish);
		this.notifyUpdate();
	}
	
	public void removeDish(Dish dish) {
		dishes.remove(dish);
		this.notifyUpdate();
	}
	@Override
	public void addUpdateListener(UpdateListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void notifyUpdate() {
		this.listeners.forEach(listener -> listener.updated(new UpdateEvent()));
	}
		
	public void setRegisteredUser(User user) {
		this.registeredUser = user;
	}
	
	public User getRegisteredUser() {
		return registeredUser;
	}
}
