package net.donotturnoff.jhttp;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.util.logging.*;

public class Response {
	
	private JHTTP server;
	private Connection conn;
	private Request request;
	private Socket s;
	private Config cfg;
	private Logger log;
	private DataOutputStream headerOut;
	private OutputStream bodyOut;
	private Status status;
	private Headers headers;
	private Document document;
	
	public Response(JHTTP server, Connection conn, RequestHandler handler) {
		this(server, conn, handler.getStatus(), handler.getHeaders(), handler.getDocument());
	}
	
	public Response(JHTTP server, Connection conn, Status status, Headers headers, Document document) {
		this.server = server;
		this.log = server.getLog();
		this.cfg = server.getServerConfig();
		this.conn = conn;
		this.request = conn.getRequest();
		this.s = conn.getSocket();
		this.status = status;
		this.headers = headers;
		this.document = document;
		
		send();
	}
	
	private DataOutputStream createHeaderOutputStream() throws IOException {
		
		/* Header output stream is always a plain DataOutputStream because headers cannot be compressed. */
		return new DataOutputStream(s.getOutputStream());
	}
	
	private OutputStream createBodyOutputStream() throws IOException {
		
		/* Create appropriate output stream for specified compression type. */
		String methods = request.getHeader("Accept-Encoding");
		if (methods.contains("gzip")) {
			return new GZIPOutputStream(s.getOutputStream());
		} else if (methods.contains("deflate")) {
			return new DeflaterOutputStream(s.getOutputStream());
		} else {
			return new DataOutputStream(s.getOutputStream());
		}
	}
	
	public void send() {
		try {
			
			/* Headers and body are sent on separate output streams so that if the body is compressed, the headers aren't. */
			
			/* 
			 * Header format:
			 * <protocol> <status>
			 * <key1>: <value1>
			 * <key2>: <value2>
			 * ...
			 * <keyn>: <valuen>
			 */
			String protocol = request.getProtocol();
			if (protocol.equals("")) {
				protocol = cfg.get("defaultprotocol");
			}
			
			headerOut = createHeaderOutputStream();
			log.log(Level.FINE, "Header output stream opened");
			
			headerOut.writeBytes(protocol + " " + status.getStatus() + "\r\n");
			for (Map.Entry<String, String> entry: headers.getHeaders().entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				headerOut.writeBytes(key + ": " + value + "\r\n");
			}
			headerOut.writeBytes("\r\n");
			log.log(Level.FINEST, "Headers written");
			
			/* Body output stream may be a compressed stream. */
			bodyOut = createBodyOutputStream();
			log.log(Level.FINE, "Body output stream opened");
			
			bodyOut.write(document.getData());
			log.log(Level.FINEST, "Body written");
			
			headerOut.flush();
			bodyOut.flush();
			
			log.log(Level.FINE, "Response sent");
		} catch (IOException e) {
			log.log(Level.WARNING, "Could not send response");
		} finally {
			close();
		}
	}
	
	public void close() {
		try {
			bodyOut.close();
			headerOut.close();
			log.log(Level.FINE, "Output stream closed");
		} catch (IOException e) {
			log.log(Level.WARNING, "Could not send response");
		}
	}
}
