package comp1206.sushi.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.User;

public class ClientMessageManager implements Runnable{
	
	private Client client;
	private Socket socket;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private Map<String, Order> orderRequests = new HashMap<>();
	
	public ClientMessageManager(Socket clientSocket, Client client) throws IOException{
		this.client = client;
		socket = clientSocket;
		input = new ObjectInputStream(clientSocket.getInputStream());
		output = new ObjectOutputStream(clientSocket.getOutputStream());
	}
	
	public void notifyServer(String details, Object object) {
		
		if (details.equals("Registration")) {
			try {
				registerUser((User) object);
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		else if (details.equals("Login")) {
			try {
				requestLogin((String) object);
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		else if (details.equals("Order")) {
			try {
				notifyNewOrder((Order) object);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		else if (details.equals("CancelOrder")) {
			try {
				requestCancelOrder((Order) object);
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				Object objectReceived = input.readObject();
				if (objectReceived instanceof User) {
					client.setRegisteredUser((User) objectReceived);
				}
				
				else if (objectReceived instanceof ArrayList<?>) {
					client.setDishes((ArrayList<Dish>) objectReceived);
				}
				
				else if (objectReceived instanceof Restaurant) {
					Restaurant serverRestaurant = (Restaurant) objectReceived;
					client.setRestaurantAndPostcodes(serverRestaurant);
				}
			}
			
			catch(ClassNotFoundException cnfe) {
				cnfe.printStackTrace();
			}
			
			catch(IOException ioe) {
				try {
					System.out.println("Loss connection to Server");
					releaseResources();
					System.exit(1);
				}
				
				catch(IOException ioe2) {
					
				}
			}	
		}
	}
	
	public void releaseResources() throws IOException{
		input.close();
		output.close();
		socket.close();
	}
	public void registerUser(User user) throws IOException{
		output.writeObject(user);
		output.flush();
		output.reset();
	}
	
	public void requestLogin(String credential) throws IOException{
		output.writeObject(credential);
		output.flush();
		output.reset();
	}	
	
	public void notifyNewOrder(Order order) throws IOException {
		System.out.println(order.getName());
		output.writeUnshared(order);
		output.flush();
		output.reset();
	}
	
	public void requestCancelOrder(Order orderToCancel) throws IOException {
		output.writeObject(orderToCancel);
		output.flush();
		output.reset();
	}
}
