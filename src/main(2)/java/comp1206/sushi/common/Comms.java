package comp1206.sushi.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.logging.log4j.Logger;

import comp1206.sushi.client.ClientMailBox;
import comp1206.sushi.server.Server;
import comp1206.sushi.server.ServerMailBox;

public class Comms{
	
	private Socket socket;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private InetAddress clientIP;
	private Logger logger;
	
	public Comms(Socket socket, Logger logger) {
		this.socket = socket;
		this.logger = logger;
		try {
			output = new ObjectOutputStream(this.socket.getOutputStream());
			input = new ObjectInputStream(this.socket.getInputStream());
		}
		catch(IOException e) {
			System.out.println("Something went wrong");
		}	
	}
	
	public Comms(Socket socket, InetAddress clientIP, Logger logger) {
		this(socket, logger);
		this.clientIP = socket.getInetAddress();
	}
	
	public ObjectInputStream getInputStream() {
		return input;
	}
	
	public ObjectOutputStream getOutputStream() {
		return output;
	}
	
	public InetAddress getClientIP() {
		return clientIP;
	}
	
	public void sendMessage(Object object) throws IOException {
		output.writeObject(object);
		output.flush();
		output.reset();
	}
	
	public Object receiveMessage(ServerMailBox serverMails, Server server) {
		Object objectReceived = null;
		try {
			objectReceived = input.readObject();
		}
		catch(IOException e) {
			try {
				logger.info("Lost connection to client " + clientIP);
				System.out.println("Loss connection to client " + clientIP);
				socket.close();
				output.close();
				input.close();
				clientIP = null;
				server.removeMailBoxes(serverMails);
			}
			catch(IOException e2) {
				e.printStackTrace();
			}   
		}
		catch(ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		return objectReceived;
	}
	
	public Object receiveMessage(ClientMailBox clientMail) {
		Object objectReceived = null;
		try {
			objectReceived = input.readObject();
		}
		catch(IOException e) {
			try {
				logger.info("Lost connection to server");
				System.out.println("Lost connection to server");
				socket.close();
				output.close();
				input.close();
			}
			catch(IOException e2) {
				e.printStackTrace();
			}
		}
		catch(ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		return objectReceived;
	}
}
