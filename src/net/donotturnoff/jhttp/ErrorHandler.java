package net.donotturnoff.jhttp;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

public class ErrorHandler extends RequestHandler {
	
	public ErrorHandler(JHTTP server, Request request, Status status) {
		this(server, request, status, "");
	}
	
	public ErrorHandler(JHTTP server, Request request, Status status, String message) {
		super(server);
		this.request = request;
		Host host = request.getHost();
		
		String code = status.getCode();
		String siteroot = host.get("root");
		String errorDocPath = host.get(code);
		
		/* Add custom message to list of messages. */
		ArrayList<String> messages = new ArrayList<String>();
		if (message.trim().length() > 0) {
			messages.add(message);
		}
		
		this.status = status;
		try {
			/* 
			 * If an error document is defined for the given code, handle it like a normal resource.
			 * To do: use ResourceHandler?
			 */
			if (!errorDocPath.equals("")) {
				if (status.isError()) {
					Path path = Paths.get(siteroot + errorDocPath);
					RequestHandler handler;
					if (!Files.exists(path)) {
						throw new IOException();
					}
					if (Files.isDirectory(path)) {
						handler = new DirectoryHandler(server, path, request, Files.isSymbolicLink(path));
					} else {
						handler = new FileHandler(server, path, request, Files.isSymbolicLink(path));
					}
					headers = handler.getHeaders();
					document = handler.getDocument();
					log.log(Level.FINER, "Read error document for " + code + " from " + siteroot + errorDocPath);
				} else {
					throw new IllegalArgumentException("Cannot send error document for non-error code " + code);
				}
			} else {
				throw new IOException();
			}
		} catch (Exception e) {
			log.log(Level.FINER, "Could not read error document for " + code, e);
			try {
				
				/* If the defined error document cannot be read, add a new error message to the message list. */
				if (!errorDocPath.equals("")) {
					String newMessage = "Could not read " + errorDocPath + ": " + e.toString();
					if (newMessage != null) {
						messages.add(newMessage);
					}
				}
			} catch (IllegalArgumentException e2) {
				log.log(Level.FINEST, "Could not create error document for non-error code " + code, e2);
				String newMessage2 = "Could not create error document: " + e2.toString();
				if (newMessage2 != null) {
					messages.add(newMessage2);
				}
				status = new Status("500");
			} finally {
				
				/* 
				 * Produce a default ErrorDocument to send in place of a custom one.
				 * This will display the status and any additional messages.
				 */
				document = new ErrorDocument(server, status, messages);
				headers = new Headers();
				headers.setHeader("Content-Type", document.getType());
				headers.setHeader("Content-Length", document.getLength());
				addGeneralHeaders();
			}
		}
	}
}
