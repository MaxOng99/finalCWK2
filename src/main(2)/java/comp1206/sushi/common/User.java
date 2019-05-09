package comp1206.sushi.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User extends Model implements Serializable{
	
	private static final long serialVersionUID = 2182220855827176304L;
	private String name;
	private String password;
	private String address;
	private Postcode postcode;
	private Basket basket;
	private List<Order> orders;
	
	public User(String username, String password, String address, Postcode postcode) {
		this.name = username;
		this.password = password;
		this.address = address;
		this.postcode = postcode;
		this.basket = new Basket();
		this.orders = new ArrayList<>();
	}
	
	public String getName() {
		return name;
	}
	
	public void addNewOrder(Order order) {
		orders.add(order);
	}
	
	public List<Order> getOrders() {
		return orders;
	}
	
	public void setName(String name) {
		this.notifyUpdate("Name", this.name, name);
		this.name = name;
	}
	
	public Number getDistance() {
		return postcode.getDistance();
	}

	public Postcode getPostcode() {
		return this.postcode;
	}
	
	public void setPostcode(Postcode postcode) {
		this.notifyUpdate("Postcode", this.postcode , postcode );
		this.postcode = postcode;
	}
	
	public void updateBasket(Basket basket) {
		
	}
	
	public Basket getBasket() {
		return basket;
	}
	
	public String getUsername() {
		return name;
	}
	
	public String getPassword() {
		return password;
	}
}
