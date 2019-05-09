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
		this.status = "Incomplete";
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
	
	public boolean equals(Object o) {
	    if (o == this) {
	      return true;
	    }
	    if (!(o instanceof Order)) {
	      return false;
	    }
	    Order orderInstance = (Order)o;
	    return orderInstance.name.equals(name) &&
	    		orderInstance.user.equals(user) &&
	    		(int)orderInstance.getCost() == (int)cost &&
	    		orderInstance.getBasket().getBasketMap().equals(basket.getBasketMap());
	}
	 
	  public int hashCode() {
	    int result = 17;
	    result = 31 * result + name.hashCode();
	    result = 31 * result + user.hashCode();
	    result = 31 * result + (int)cost;
	    result = 31 * result + basket.getBasketMap().hashCode();
	    return result;
	}
}
