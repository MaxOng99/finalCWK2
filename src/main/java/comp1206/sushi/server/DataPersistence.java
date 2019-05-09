package comp1206.sushi.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import comp1206.sushi.common.Drone;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Postcode;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.Staff;
import comp1206.sushi.common.Supplier;
import comp1206.sushi.common.User;

public class DataPersistence {	
	
	private File dishesFile = new File("src/main/resources/persistence/dishes.txt");
	private File ingredientsFile = new File("src/main/resources/persistence/ingredients.txt");
	private File suppliersFile = new File("src/main/resources/persistence/suppliers.txt");
	private File ordersFile= new File("src/main/resources/persistence/orders.txt");
	private File staffsFile = new File("src/main/resources/persistence/staffs.txt");
	private File postcodesFile = new File("src/main/resources/persistence/postcodes.txt");
	private File usersFile = new File("src/main/resources/persistence/users.txt");
	private File dronesFile = new File("src/main/resources/persistence/drones.txt");
	private File restaurantFile = new File("src/main/resources/persistence/restaurant.txt");
	private List<File> persistentFiles = new ArrayList<>();
	private ObjectOutputStream dishesOS;
	
	private ObjectInputStream dishesIS;
	private ObjectOutputStream ingredientsOS;
	private ObjectInputStream ingredientsIS;
	private ObjectOutputStream suppliersOS;
	private ObjectInputStream suppliersIS;
	private ObjectOutputStream ordersOS;
	private ObjectInputStream ordersIS;
	private ObjectOutputStream staffsOS;
	private ObjectInputStream staffsIS;
	private ObjectOutputStream postcodesOS;
	private ObjectInputStream postcodesIS;
	private ObjectOutputStream usersOS;
	private ObjectInputStream usersIS;
	private ObjectOutputStream dronesOS;
	private ObjectInputStream dronesIS;
	private ObjectOutputStream restaurantOS;
	private ObjectInputStream restaurantIS;
	private Server server;
	
	
	public DataPersistence(Server server) {
		this.server = server;
		persistentFiles.add(dishesFile);
		persistentFiles.add(ingredientsFile);
		persistentFiles.add(suppliersFile);
		persistentFiles.add(ordersFile);
		persistentFiles.add(staffsFile);
		persistentFiles.add(postcodesFile);
		persistentFiles.add(usersFile);
		persistentFiles.add(dronesFile);
		persistentFiles.add(restaurantFile);
		
		if (persistentFilesExist()) {
			try {
				dishesIS = new ObjectInputStream(new FileInputStream(dishesFile));
				ingredientsIS = new ObjectInputStream(new FileInputStream(ingredientsFile));
				suppliersIS = new ObjectInputStream(new FileInputStream(suppliersFile));
				ordersIS = new ObjectInputStream(new FileInputStream(ordersFile));
				staffsIS = new ObjectInputStream(new FileInputStream(staffsFile));
				postcodesIS = new ObjectInputStream(new FileInputStream(postcodesFile));
				usersIS = new ObjectInputStream(new FileInputStream(usersFile));
				dronesIS = new ObjectInputStream(new FileInputStream(dronesFile));
				restaurantIS = new ObjectInputStream(new FileInputStream(restaurantFile));
				readSavedData();				
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean persistentFilesExist() {
		
		boolean filesExist = true;
		for (File file: persistentFiles) {
			if (!file.exists()) {
				filesExist = false;
				break;
			}
		}
		
		return filesExist;
	}
	
	public void readRestaurant() {
		try {
			Restaurant savedRestaurant = (Restaurant) restaurantIS.readObject();
			server.setRestaurantFromConfig(savedRestaurant);
			restaurantIS.close();
			writeRestaurant(savedRestaurant);
		}
		catch(ClassNotFoundException | IOException e) {
			return;
		}
	}
	
	public void readPostcodes() {
		List<Postcode> savedPostcodes;
		try {
			savedPostcodes = (List<Postcode>) postcodesIS.readObject();
			server.setPostcodes(savedPostcodes);
			postcodesIS.close();
		} catch (ClassNotFoundException | IOException e) {
			return;
		}
	}
	
	public void readSuppliers() {
		List<Supplier> savedSuppliers;
		try {
			savedSuppliers = (List<Supplier>) suppliersIS.readObject();
			server.setSuppliers(savedSuppliers);
			suppliersIS.close();
		} catch (ClassNotFoundException | IOException e) {
			return;
		}
	}
	
	public void readIngredientManager() {
		IngredientStockManager savedIM;
		try {
			savedIM = (IngredientStockManager) ingredientsIS.readObject();
			server.setIngredientManager(savedIM);
			writeIngredientManager(server.getIngredientManager());
			ingredientsIS.close();
		} catch (ClassNotFoundException | IOException e) {
			return;
		}
	}
	
	public void readDishManager() {
		DishStockManager savedDM;
		try {
			savedDM = (DishStockManager) dishesIS.readObject();
			server.setDishManager(savedDM);
			this.writeDishManager(server.getDishManager());
			dishesIS.close();
		} catch (ClassNotFoundException | IOException e) {
			return;
		}
	}
	
	public void readUsers() {
		List<User> savedUsers;
		try {
			savedUsers = (List<User>) usersIS.readObject();
			server.setUsers(savedUsers);
			usersIS.close();
		} catch (ClassNotFoundException | IOException e) {
			return;
		}
	}
	
	public void readOrders() {
		List<Order> savedOrders;
		try {
			savedOrders = (List<Order>) ordersIS.readObject();
			server.setSavedOrders(savedOrders);
			writeDishManager(server.getDishManager());
			writeIngredientManager(server.getIngredientManager());
			writeOrders(server.getOrders());
			ordersIS.close();
		} catch (ClassNotFoundException | IOException e) {
			return;
		}	
	}
	
	public void readStaff() {
		
		try {
			List<String> savedStaffName = (List<String>) staffsIS.readObject();
			List<Staff> savedStaffs = new ArrayList<>();
			for (String staffName: savedStaffName) {
				savedStaffs.add(new Staff(staffName, server));
			}
			server.setStaffsFromConfig(savedStaffs);
			server.setStaffData(savedStaffName);
			staffsIS.close();
		}catch (ClassNotFoundException | IOException e) {
			return;
		}	
	}
	
	public void readDrones() {
		try {
			List<Integer> savedDroneSpeed = (List<Integer>) dronesIS.readObject();
			List<Drone> savedDrones = new ArrayList<>();
			for (Integer droneSpeed: savedDroneSpeed) {
				savedDrones.add(new Drone(droneSpeed));
			}
			server.setDronesFromConfig(savedDrones);
			server.setDroneData(savedDroneSpeed);
			dronesIS.close();
		}catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
	public void readSavedData() {
		readRestaurant();
		readPostcodes();
		readSuppliers();
		readIngredientManager();
		readDishManager();
	}
	
	public void writeRestaurant(Restaurant restaurant) {
		try {
			restaurantOS = new ObjectOutputStream(new FileOutputStream(restaurantFile));
			restaurantOS.writeObject(restaurant);
			restaurantOS.close();
		}
		catch(IOException e) {
			return;
		}
	}
	public synchronized void writeDishManager(DishStockManager dishManager) {
		try {
			dishesOS = new ObjectOutputStream(new FileOutputStream(dishesFile));
			dishesOS.writeObject(dishManager);
			dishesOS.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void writeIngredientManager(IngredientStockManager ingredientManager) {
		try {
			ingredientsOS = new ObjectOutputStream(new FileOutputStream(ingredientsFile));
			ingredientsOS.writeObject(ingredientManager);
			ingredientsOS.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void writeDrones(List<Integer> drones) {
		try {
			dronesOS = new ObjectOutputStream(new FileOutputStream(dronesFile));
			dronesOS.writeObject(drones);
			dronesOS.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void writeOrders(List<Order> orders) {
		try {
			ordersOS = new ObjectOutputStream(new FileOutputStream(ordersFile));
			ordersOS.writeObject(orders);
			ordersOS.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void writePostcodes(List<Postcode> postcodes) {
		try {
			postcodesOS = new ObjectOutputStream(new FileOutputStream(postcodesFile));
			postcodesOS.writeObject(postcodes);
			postcodesOS.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void writeStaffs(List<String> staffs) {
		try {
			staffsOS = new ObjectOutputStream(new FileOutputStream(staffsFile));
			staffsOS.writeObject(staffs);
			staffsOS.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void writeSuppliers(List<Supplier> suppliers) {
		try {
			suppliersOS = new ObjectOutputStream(new FileOutputStream(suppliersFile));
			suppliersOS.writeObject(suppliers);
			suppliersOS.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void writeUsers(List<User> users) {
		try {
			usersOS = new ObjectOutputStream(new FileOutputStream(usersFile));
			usersOS.writeObject(users);
			usersOS.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
