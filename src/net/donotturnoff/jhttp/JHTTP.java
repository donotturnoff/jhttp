package net.donotturnoff.jhttp;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.naming.ConfigurationException;

public class JHTTP {

	public static void main(String[] args) {
		JHTTP server = new JHTTP();
		server.start();
	}
	
	public final static String version = "1.0";
	public final static String server = "JHTTP/" + version;
	
	private ServerSocket s;
	private Socket c;
	private final Logger log;
	private Config serverConfig;
	private HashMap<String, Host> hosts;
	private final ArrayList<Connection> connections;
	
	public JHTTP() {
		log = Logger.getLogger(JHTTP.class.getName());
		log.setLevel(Level.ALL);
		loadConfig();
		
		String logfile = serverConfig.get("log");
		
		try {
			log.addHandler(new java.util.logging.FileHandler(logfile));
		} catch (IOException e) {
			log.log(Level.WARNING, "Could not open log file", e);
			log.log(Level.INFO, "Resorting to console logging only");
		}
		
		/* Check that the root directory of every virtual host defined in the config file exists and is readable. */
		for (Host host: hosts.values()) {
			String root = host.get("root");
			File rootDir = new File(root);
			if (!(rootDir.isDirectory() && rootDir.canRead())) {
				log.log(Level.SEVERE, "Could not read www directory for " + host.getHostname());
				exit(3);
			}
		}
		
		connections = new ArrayList<>(20);
	}
	
	private void loadConfig() {
		ConfigHandler handler;
		try {
			handler = new ConfigHandler("jhttp.xml");
			serverConfig = handler.getServerConfig();
			hosts = handler.getHosts();
			log.log(Level.INFO, "Loaded config file");
		} catch (ConfigurationException e) {
			log.log(Level.SEVERE, "Error while parsing config file", e);
			exit(1);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Could not open config file", e);
			exit(1);
		}
	}
	
	public Logger getLog() {
		return log;
	}
	
	public Config getServerConfig() {
		return serverConfig;
	}
	
	public HashMap<String, Host> getHosts() {
		return hosts;
	}
	
	public Host getHost(String hostname) {
		Host host = hosts.get(hostname);
		if (host == null) {
			String hostnameWithoutPort = hostname.split(":")[0];
			host = hosts.get(hostnameWithoutPort);
			if (host == null) {
				return hosts.get(serverConfig.get("defaultHost"));
			} else {
				return host;
			}
		} else {
			return host;
		}
	}
	
	public void start() {
		/* Determine port to start server on and handle any errors which may arise. */
		int port = 0;
		try {
			port = Integer.parseInt(serverConfig.get("port"));
			s = new ServerSocket(port);
			log.log(Level.INFO, "Server started on port " + port + " as " + System.getProperty("user.name"));
		} catch (IOException | NumberFormatException e) {
			log.log(Level.SEVERE, "Could not start server on port " + port, e);
			exit(2);
		}

		/*
		 * Main loop of server:
		 * 1. Accept new connection
		 * 2. Create new threaded Connection object to handle it
		 * 3. Start connection handling
		 */
		//noinspection InfiniteLoopStatement
		while (true) {
			try {
				Socket client = s.accept();
				Connection c = new Connection(this, client);
				connections.add(c);
				c.start();
			} catch (IOException e) {
				log.log(Level.WARNING, "Could not accept connection", e);
			}
		}
	}
	
	public void close(Connection c) {
		c.close();
		connections.remove(c);
	}
	
	private void clear() {
		for (Connection c: connections) {
			close(c);
		}
	}
	
	private void exit(int returnValue) {
		try {
			clear();
			s.close();
			log.log(Level.INFO, "Server shut down successfully");
		} catch (IOException e) {
			log.log(Level.WARNING, "Could not shut server down", e);
			returnValue = 5;
		} finally {
			log.log(Level.INFO, "Exiting with status " + returnValue);
		}
		System.exit(returnValue);
	}
}
