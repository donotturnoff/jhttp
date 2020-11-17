package net.donotturnoff.jhttp;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.logging.*;

public class ErrorDocument extends Document {
	
	private Status status;
	private ArrayList<String> messages;
	
	public ErrorDocument(JHTTP server, Status status) {
		this(server, status, new ArrayList<String>());
	}
	
	public ErrorDocument(JHTTP server, Status status, ArrayList<String> messages) throws IllegalArgumentException {
		super(server);
		
		this.status = status;
		this.messages = messages;
		
		/* Produce error document if the given status code is 4xx or 5xx and is a valid code. */
		if (status.isError()) {
			StringBuilder docBuilder = new StringBuilder();
			docBuilder.append("<!DOCTYPE html>\n");
			docBuilder.append("<html>\n");
			docBuilder.append(" <head>\n");
			docBuilder.append("  <meta charset=\"UTF-8\" />\n");
			docBuilder.append("  <title>Error ");
			docBuilder.append(status.getCode());
			docBuilder.append("</title>\n");
			docBuilder.append(" </head>\n");
			docBuilder.append(" <body>\n");
			docBuilder.append("  <h1>Error ");
			docBuilder.append(status.getCode());
			docBuilder.append("</h1>\n");
			docBuilder.append("  <p>");
			docBuilder.append(status.getMessage());
			docBuilder.append("</p>\n");
			
			/* If there are extra messages, list them. */
			if (messages.size() > 0) {
				docBuilder.append("  <h2>Additional information</h2>\n");
				docBuilder.append("  <ul>\n");
				for (String message: messages) {
					docBuilder.append("   <li>");
					docBuilder.append(message);
					docBuilder.append("</li>\n");
				}
				docBuilder.append("  </ul>\n");
			}
			
			docBuilder.append(" </body>\n");
			docBuilder.append("</html>\n");
			data = docBuilder.toString().getBytes();
			type = "text/html";
			log.log(Level.FINEST, "Created error document for " + status.getCode());
		} else {
			throw new IllegalArgumentException("Cannot create error document for non-error code " + status.getCode());
		}
	}
}
