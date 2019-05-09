package comp1206.sushi.server;

import java.net.InetAddress;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.Logger;

public class ClientListener implements Runnable{
	
	private Server server;
	private Logger logger;
	private ExecutorService executors;
	
	public ClientListener(Server server, Logger logger) {
		this.server = server;
		this.logger = logger;
	}
	
	public void run() {
		try {
			listenForClients();
		}
		catch(Exception e) {
			shutDownExecutor();
			return;
		}
	}
	
	public void shutDownExecutor() {
		executors.shutdownNow();
	}
	
	public void listenForClients() throws Exception{
		ServerSocket serverSocket = new ServerSocket(49920);
		executors = Executors.newFixedThreadPool(10);
		Socket socket = null;
		InetAddress clientIP = null;
		while (!Thread.currentThread().isInterrupted()) {
			
			if (Thread.interrupted()) {
				return;
			}
			
			else {
				socket = serverSocket.accept();
				clientIP = socket.getInetAddress();
				ServerMailBox newMailBox = new ServerMailBox(server, socket, clientIP, logger);
				server.addMailBoxes(newMailBox);
				executors.execute((newMailBox));
				if (socket != null && socket.isConnected()) {
					logger.info("Connected to client " + clientIP);
				}
			}
		}
	}
}