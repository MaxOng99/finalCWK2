package comp1206.sushi.server;

import java.io.IOException;


import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.Logger;

import comp1206.sushi.common.Comms;
import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.User;

public class ServerMailBox implements Runnable{
	
	private boolean closeMailBox = false;
	private Server server;
	private Comms serverSideComm;
	private InetAddress clientIP;
	private User registeredUser;
	private List<Order> userOrders;
	private List<Order> serverOrders;
	
	public ServerMailBox(Server server, Socket socket, InetAddress clientIP, Logger logger) {
		this.server = server;
		this.serverSideComm = new Comms(socket, clientIP, logger);
		this.clientIP = clientIP;
		this.userOrders = Collections.synchronizedList(new ArrayList<>());
		this.serverOrders = server.getOrders();
	}
	
	private void verifyCredentials(String credentials) throws IOException{
		String[] credential = credentials.split(":");
		User userToVerify = null;
		
		synchronized(server.getUsers()) {
			for (User user: server.getUsers()) {
				if (credential[0].equals(user.getUsername()) && credential[1].equals(user.getPassword())) {
					userToVerify = user;
					
					for (Order order: server.getOrders()) {
						if (order.getUser().equals(userToVerify)) {
							userToVerify.addNewOrder(order);
						}
					}
					
					serverSideComm.sendMessage(userToVerify);
					break;
				}
			}
			if (userToVerify == null) {
				serverSideComm.sendMessage("Verification Failed");
			}
		}
	}
	
	public void terminateClient() {
		try {
			closeMailBox = true;
			System.out.println("Called to close mailBoxes");
			serverSideComm.sendMessage("Terminate Client");
		} catch (IOException e) {
			return;
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
		server.addOrder(order);
		userOrders.add(order);
	}
	
	private void cancelOrder(Order order) {
		synchronized(serverOrders) {
			Iterator<Order> orderIt = serverOrders.iterator();
			while(orderIt.hasNext()) {
				Order currentOrder = orderIt.next();
				if (currentOrder.equals(order)) {
					currentOrder.setStatus("Canceled");
					break;
				}
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
			return;
		}
		
		while (serverSideComm.getClientIP() != null && closeMailBox == false) {
			
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
	}
	
	private class InformCompleteOrder implements Runnable {
		@Override
		public void run() {
			List<Order> ordersToRemove = new ArrayList<>();
			while(closeMailBox == false) {
				if (!userOrders.isEmpty()) {
					synchronized (userOrders) {
						Iterator<Order> orderIt = userOrders.iterator();
						while(orderIt.hasNext()) {
							Order order = orderIt.next();
							if (server.isOrderComplete(order)) {
								try {
									ordersToRemove.add(order);
									serverSideComm.sendMessage(order);
								}
								catch(IOException e) {
									return;
								}
							}
						}
						userOrders.removeAll(ordersToRemove);
						ordersToRemove.clear();
					}
				}
			}	
		}
	}
}
