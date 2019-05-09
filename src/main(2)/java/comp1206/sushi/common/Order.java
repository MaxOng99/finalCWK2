package comp1206.sushi.common;

import java.io.Serializable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import comp1206.sushi.common.Order;

public class Order extends Model implements Serializable {
	
	private static final long serialVersionUID = -5759545672682796841L;
	private volatile String status;
	private Number cost;
	private User user;
	private Basket basket;
	private String name;
	
	public Order(User user) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss");  
		LocalDateTime now = LocalDateTime.now();  
		this.name = dtf.format(now);
		this.user = user;
		this.basket = this.user.getBasket();
		this.cost = this.basket.getCost();
	}

	public Number getDistance() {
		return user.getDistance();
	}

	@Override
	public String getName() {
		return this.name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		notifyUpdate("Order Status",this.status,status);
		this.status = status;
	}
	
	public Number getCost() {
		return cost;
	}
	
	public User getUser() {
		return user;
	}
	
	public Basket getBasket() {
		return basket;
	}
}
