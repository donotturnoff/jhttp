package net.donotturnoff.jhttp;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.logging.*;

public class Request {
	
	private final JHTTP server;
	private final Connection conn;
	private final Logger log;
	private BufferedReader in;
	private String request;
	private final ArrayList<String> lines;
	private String verb, path, queryString, protocol, body;
	private final Headers headers;
	private Host host;
	
	public Request(JHTTP server, Connection conn) {
		this.server = server;
		this.log = server.getLog();
		Config cfg = server.getServerConfig();
		this.conn = conn;
		Socket s = conn.getSocket();
		
		request = "";
		lines = new ArrayList<>(20);
		headers = new Headers();
		
		verb = "";
		path = "";
		protocol = "";
		
		try {
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			log.log(Level.FINE, "Input stream opened");
		} catch (IOException e) {
			log.log(Level.WARNING, "Could not open input stream", e);
		}
	}
	
	public void handle() {
		RequestHandler handler = null;
		try {
			int length = 0;
			String line;
			StringBuilder requestBuilder = new StringBuilder();
			
			/* 
			 * Read all request headers and use Content-Length header to determine amount of body to read.
			 * To do: handle incoming requests without Content-Length header (i.e. read until end).
			 */
			try {
				while (!(line = in.readLine()).equals("")) {
					if (line.split(":")[0].equals("Content-Length")) {
						try {
							length = Integer.parseInt(line.split(":")[1].trim());
							if (length < 0) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException e) {
							e.printStackTrace();
							length = 0;
						}
					}
					requestBuilder.append(line).append("\r\n");
					lines.add(line);
				}
			} catch (NullPointerException e) {
				requestBuilder.append("\r\n");
			}
			
			/* Read body into char buffer. */
			char[] bodyText = new char[length];
			in.read(bodyText, 0, length);
			
			body = new String(bodyText);
			requestBuilder.append(bodyText);
			
			request = requestBuilder.toString();
			log.log(Level.FINE, "Received request: " + request);
			
			/* Parse and handle request. */
			parse();
			handler = new RequestHandler(server, this);
		} catch (SocketTimeoutException e) {
			log.log(Level.INFO, "Connection timed out");
			handler = new ErrorHandler(server, this, new Status("408"));
		} catch (IOException e) {
			log.log(Level.WARNING, "Could not receive request", e);
			handler = new ErrorHandler(server, this, new Status("500"), "Could not receive request");
		} finally {
			if (handler == null) {
				handler = new ErrorHandler(server, this, new Status("500"), "RequestHandler is null");
			}
			Response response = new Response(server, conn, handler);
		}
	}
	
	private void parse() {
		final String requestLineRegex = "(GET|HEAD|POST|PUT|DELETE|CONNECT|OPTIONS|TRACE|PATCH)\\s+([^?\\s]+)((?:[?&][^&\\s]+)*)\\s+(HTTP/\\d(\\.\\d)?)"; // Matches valid request line
		final String headerRegex = "([\\w-]+): (.*)"; // Matches valid header ("key: value" pair)
		Pattern requestLinePattern = Pattern.compile(requestLineRegex);
		Pattern headerPattern = Pattern.compile(headerRegex);
		for (String line: lines) {
			Matcher requestLineMatcher = requestLinePattern.matcher(line);
			Matcher headerMatcher = headerPattern.matcher(line);
			
			if (headerMatcher.find()) {
				String[] parts = line.split(":");
				String key = parts[0].trim();
				String[] tail = Arrays.copyOfRange(parts, 1, parts.length);
				String value = String.join(":", tail).trim(); 
				headers.setHeader(key, value);
				log.log(Level.FINER, "Header encountered: " + line);
			} else if (requestLineMatcher.find()) {
				
				/* Extract verb, path, parameters and protocol from request path. */
				String[] parts = line.split(" ");
				
				verb = parts[0].trim();
				
				/* Convert path to URL so the actual path and the query string can be separated. */
				String urlString = parts[1].trim().replaceAll("/+$", "");
				urlString = "http://" + urlString; // Add http:// just to make it a valid URL
				try {
					URL url = new URL(urlString);
					path = url.getPath();
					queryString = url.getQuery();
					if (queryString == null) {
						queryString = "";
					}
				} catch (MalformedURLException e) {
					path = "";
					queryString = "";
				}
				
				protocol = parts[2].trim();
				
				log.log(Level.FINER, "Request line encountered: " + line);
			} else {
				log.log(Level.FINER, "Unexpected header encountered: " + line);
			}
		}
		log.log(Level.FINE, "Parsed request");
		host = server.getHost(getHeader("Host"));
	}
	
	public Connection getConnection() {
		return conn;
	}
	
	public Host getHost() {
		return host;
	}
	
	public String getVerb() {
		return verb;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getQueryString() {
		return queryString;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	public Headers getHeaders() {
		return headers;
	}
	
	public String getHeader(String key) {
		return headers.getHeader(key);
	}
	
	public String getBody() {
		return body;
	}
	
	public void close() {
		try {
			in.close();
			log.log(Level.FINE, "Input stream closed");
		} catch (IOException e) {
			log.log(Level.WARNING, "Could not close input stream", e);
		}
	}
}
