package comp1206.sushi.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.Logger;

import comp1206.sushi.common.Basket;
import comp1206.sushi.common.Comms;
import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.User;

public class ServerMailBox implements Runnable{
	
	private Server server;
	private Comms serverSideComm;
	private InetAddress clientIP;
	private User registeredUser;
	private List<Order> orders;
	
	public ServerMailBox(Server server, Socket socket, InetAddress clientIP, Logger logger) {
		this.server = server;
		this.serverSideComm = new Comms(socket, clientIP, logger);
		this.clientIP = clientIP;
		this.orders = new CopyOnWriteArrayList<>();
	}
	
	private void verifyCredentials(String credentials) throws IOException{
		String[] credential = credentials.split(":");
		User userToVerify = null;
		for (User user: server.getUsers()) {
			if (credential[0].equals(user.getUsername()) && credential[1].equals(user.getPassword())) {
				userToVerify = user;
				serverSideComm.sendMessage(user);
				break;
			}
		}
		if (userToVerify == null) {
			serverSideComm.sendMessage("Verification Failed");
		}
	}
	
	public void sendInitialDataToClient() throws IOException{
		if (clientIP != null) {
			ArrayList<Dish> serverDishes = new ArrayList<>(server.getDishes());
			serverSideComm.sendMessage(serverDishes);
			serverSideComm.sendMessage(server.getRestaurant());
		}
		else {
			return;
		}
	}
	
	public void sendNewDish(Dish dish) throws IOException {
		serverSideComm.sendMessage(dish);
	}
	
	private void addUser(User user) {
		registeredUser = user;
		server.addUser(user);
	}
	
	private void addNewOrder(Order order) {
		User userOfNewOrder = order.getUser();
		String username = userOfNewOrder.getName();
		String password = userOfNewOrder.getPassword();
		for (User user: server.getUsers()) {
			if (user.getName().equals(username) && user.getPassword().equals(password)) {
				Basket basket = order.getBasket();
				user.updateBasket(basket);
				user.addNewOrder(order);
				server.addOrder(order);
				this.orders.add(order);
				break;   
			}
		}
	}
	
	private void cancelOrder(Order order) {
		for (Order current: server.getOrders()) {
			if(current.getName().equals(order.getName())) {
				current.setStatus("Canceled");
				break;
			}
		}
	}
	
	public User getRegisteredUser() {
		return registeredUser;
	}
	
	@Override
	public void run() {
		try {
			sendInitialDataToClient();
			Thread informCompleteOrderThread = new Thread(new InformCompleteOrder());
			informCompleteOrderThread.start();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		while (true) {
			if (serverSideComm.getClientIP() != null) {
				Object objectReceived = serverSideComm.receiveMessage(this, server);
				
				if (objectReceived instanceof User) {
					addUser((User) objectReceived);
				}
				
				else if (objectReceived instanceof Order) {
					Order order = (Order) objectReceived;
				
					if (order.getStatus().equals("Incomplete")) {
						addNewOrder(order);
					}
					
					else if (order.getStatus().equals("Canceled")) {
						cancelOrder(order);
					}
				}	
				
				else if (objectReceived instanceof String) {
					String credentials = (String) objectReceived;
					if (credentials.contains(":")) {
						try {
							verifyCredentials(credentials);
						}
						
						catch(IOException verificationFailed) {
							verificationFailed.printStackTrace();
						}
					}
				}
			}
			else {
				break;
			}
		}
	}
	
	private class InformCompleteOrder implements Runnable {
		@Override
		public void run() {
			List<Order> ordersToRemove = new ArrayList<>();
			while(true) {
				if (!ServerMailBox.this.orders.isEmpty()) {
					Iterator<Order> orderIt = orders.iterator();
					while(orderIt.hasNext()) {
						Order order = orderIt.next();
						if (server.isOrderComplete(order)) {
							try {
								ordersToRemove.add(order);
								serverSideComm.sendMessage(order);
								System.out.println("Completed Order message sent");
							}
							catch(IOException e1) {
								e1.printStackTrace();
							}
						}
					}
					ServerMailBox.this.orders.removeAll(ordersToRemove);
				}
			}	
		}
	}
}
