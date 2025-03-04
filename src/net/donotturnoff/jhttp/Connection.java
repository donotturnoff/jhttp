package net.donotturnoff.jhttp;

import java.net.*;
import java.io.*;
import java.util.logging.*;

public class Connection extends Thread {
	
	private final JHTTP server;
	private final Socket s;
	private Request request;
	private final Logger log;
	
	public Connection(JHTTP server, Socket s) {
		this.server = server;
		this.log = server.getLog();
		Config cfg = server.getServerConfig();
		this.s = s;
		
		try {
			int timeout = Integer.parseInt(cfg.get("timeout"));
			s.setSoTimeout(timeout);
		} catch (SocketException e) {
			log.log(Level.WARNING, "Could not set socket timeout");
		}
		
		log.log(Level.INFO, "Connection accepted from " + getHost());
	}
	
	public void run() {
		/* Handles request in a new thread. */
		request = new Request(server, this);
		request.handle();
		server.close(this);
	}
	
	public Socket getSocket() {
		return s;
	}
	
	public Request getRequest() {
		return request;
	}
	
	public String getHost() {
		return s.getInetAddress().getCanonicalHostName();
	}
	
	public void close() {
		try {
			request.close();
			s.close();
			log.log(Level.INFO, "Socket closed");
		} catch (IOException e) {
			log.log(Level.WARNING, "Could not close socket");
		}
	}
}
