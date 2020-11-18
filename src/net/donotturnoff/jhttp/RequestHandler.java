package net.donotturnoff.jhttp;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

public class RequestHandler {
	
	protected JHTTP server;
	protected Config cfg;
	protected Logger log;
	protected Request request;
	protected Status status;
	protected Headers headers;
	protected Document document;
	
	public RequestHandler() {}
	
	public RequestHandler(JHTTP server) {
		this.server = server;
		this.log = server.getLog();
		this.cfg = server.getServerConfig();
	}
	
	public RequestHandler(JHTTP server, Request request) {
		this(server);
		this.request = request;
		
		RequestHandler handler;
		String verb = request.getVerb();
		if (validateSyntax()) {
			
			/*
			 * If the request method can be handled, handle it.
			 * If the method is valid but cannot be handled yet, issue a 501 Not Implemented error.
			 * If the method is invalid, issue a 400.
			 */
			if (verb.equals("GET") || verb.equals("HEAD") || verb.equals("POST") || verb.equals("PUT")) {
				handler = new ResourceHandler(server, request);
			} else if (verb.equals("DELETE") || verb.equals("CONNECT") || verb.equals("OPTIONS") || verb.equals("TRACE") || verb.equals("PATCH")) {
				handler = new ErrorHandler(server, request, new Status("501"));
			} else {
				handler = new ErrorHandler(server, request, new Status("400"));
			}
		} else {
			handler = new ErrorHandler(server, request, new Status("400"));
		}
		status = handler.getStatus();
		headers = handler.getHeaders();
		document = handler.getDocument();
		
		if (verb.equals("HEAD")) {
			document = new Document();
		}
		
		setSpecificStatus();
		addSpecificHeaders();
		addGeneralHeaders();
	}
	
	private boolean validateSyntax() {
		/* To do: add more validation checks. */
		if (request.getProtocol().equals("HTTP/1.1")) {
			/* If using HTTP 1.1, the Host header must be set. */
			return request.getHeader("Host").length() > 0;
		} else {
			return true;
		}
	}
	
	public Document getDocument() {
		return document;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public Headers getHeaders() {
		return headers;
	}
	
	protected void setSpecificStatus() {
		
		/*
		 * If a specific status code is set for a host or a document in the config, apply it here.
		 * This overrides the status produced by the Handler.
		 */
		
		Host host = request.getHost();
		String hostSpecificStatus = host.getHostConfig().get("status");
		if (hostSpecificStatus.length() > 0) {
			status = new Status(hostSpecificStatus);
		}
		
		String path = request.getPath();
		Config documentConfig = host.getDocumentConfig(path);
		String docSpecificStatus = documentConfig.get("status");
		if (docSpecificStatus.length() > 0) {
			status = new Status(docSpecificStatus);
		}
	}
	
	protected void addGeneralHeaders() {
		handleCompression();
		headers.setHeader("Server", JHTTP.server);
		headers.setHeader("Date", getDateString());
	}
	
	protected void addSpecificHeaders() {
		
		/*
		 * If specific headers are set for a host or a document in the config, apply them here.
		 * This can override the headers produced by the Handler.
		 */
		 
		Host host = request.getHost();
		Config hostConfig = host.getHostConfig();
		String[] hostHeaderStrings = hostConfig.get("headers").split("\r\n");
		for (String headerString: hostHeaderStrings) {
			String[] headerParts = headerString.split(":");
			if (headerParts.length == 2) {
				headers.setHeader(headerParts[0], headerParts[1]);
			}
		}
		
		String path = request.getPath();
		Config documentConfig = host.getDocumentConfig(path);
		String[] docHeaderStrings = documentConfig.get("headers").split("\r\n");
		for (String headerString: docHeaderStrings) {
			String[] headerParts = headerString.split(":");
			if (headerParts.length == 2) {
				headers.setHeader(headerParts[0], headerParts[1]);
			}
		}
	}
	
	protected void handleCompression() {
		
		/* 
		 * Set the header telling the client the compression method.
		 * Also, if the data has been compressed, remove the Content-Length header (as it will be inaccurate).
		 * To do: Somehow determine length of compressed data.
		 */
		
		String methods = request.getHeader("Accept-Encoding");
		if (methods.contains("gzip")) {
			headers.setHeader("Content-Length", "");
			headers.setHeader("Content-Encoding", "gzip");
		} else if (methods.contains("deflate")) {
			headers.setHeader("Content-Length", "");
			headers.setHeader("Content-Encoding", "deflate");
		} else {
			headers.setHeader("Content-Encoding", "identity");
		}
	}
	
	protected String getDateString() {
		
		/* Produce date in format used by HTTP. */
		SimpleDateFormat dateFormat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss 'GMT'");
		Date date = new Date();
		return dateFormat.format(date);
	}
}
