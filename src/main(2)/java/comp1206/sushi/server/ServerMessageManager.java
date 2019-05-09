package comp1206.sushi.server;

import java.io.IOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.User;

public class ServerMessageManager implements Runnable{
	
	private Server server;
	private Socket serverSocket;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private InetAddress clientIP;
	
	public ServerMessageManager(Socket socket, Server server) throws IOException{
		
		this.server = server;
		this.serverSocket = socket;
		this.clientIP = socket.getInetAddress();
		this.output = new ObjectOutputStream(socket.getOutputStream());
		this.input = new ObjectInputStream(socket.getInputStream());	
	}
	
	@Override
	public void run() {
		
		while (true) {
			
			try {
				Object objectReceived = input.readObject();
				
				if (objectReceived instanceof User) {
					server.addUser((User) objectReceived);
				}
				
				else if (objectReceived instanceof String) {
					if (((String) objectReceived).contains(":")) {
						String[] credential = ((String) objectReceived).split(":");
						
						for (User user: server.getUsers()) {
							if (credential[0].equals(user.getUsername()) && credential[1].equals(user.getPassword())) {
								output.writeObject(user);
								output.flush();
							}
							
							else {
								output.writeObject(null);
								output.flush();
							}
						}
					}
				}
				
				else if (objectReceived instanceof Order) {
					Order order = (Order) objectReceived;
					
					if (order.getStatus().equals("Incomplete")) {
						server.addOrder(order);
						Map<Dish, Number> dishesOrdered = order.getUser().getBasket().getBasketMap();
						for (Entry<Dish, Number> dishQttyPair: dishesOrdered.entrySet()) {
							
							for (Dish dish: server.getDishes()) {
								if (dish.getName().equals(dishQttyPair.getKey().getName())) {
									server.setStock(dish, -(int) dishQttyPair.getValue());
								}
							}
						}
					}
					
					else if (order.getStatus().equals("Canceled")) {
						for (Order current: server.getOrders()) {
							if(current.getName().equals(order.getName())) {
								current.setStatus("Canceled");
								break;
							}
						}
					}
					
				}	
			} catch (ClassNotFoundException | IOException e) {
				try {
					System.out.println("Lost connection to Client " + clientIP);
					clientIP = null;
					output.close();
					input.close();
					serverSocket.close();
					return;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	public void notifyClient() {
		ArrayList<Dish> serverDishes2 = (ArrayList<Dish>) server.getDishes();
		try {
			output.writeObject(serverDishes2);
			output.flush();
			output.reset();
		}
		catch(IOException zzz) {
			zzz.printStackTrace();
		}
		
		Restaurant restaurant = server.getRestaurant();
		try {
			output.writeObject(restaurant);
			output.flush();
			output.reset();
		}
		
		catch(IOException eee) {
			eee.printStackTrace();
		}
	}
}

	