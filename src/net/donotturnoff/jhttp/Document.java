package net.donotturnoff.jhttp;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.logging.*;

public class Document {
	
	protected JHTTP server;
	protected Config cfg;
	protected Logger log;
	protected Path path;
	protected String type;
	protected byte[] data;
	
	public Document() {}
	
	public Document(JHTTP server) {
		this.server = server;
		this.log = server.getLog();
		this.cfg = server.getServerConfig();
		
		path = Paths.get("");
		type = cfg.get("defaultmime");
		data = new byte[0];
	}
	
	public Document(JHTTP server, Path path) throws IOException {
		this(server);
		
		this.path = path;
		type = Files.probeContentType(path);
		if (type == null) {
			type = cfg.get("defaultmime");
		}
		data = Files.readAllBytes(path);
	}
	
	public byte[] getData() {
		return data;
	}
	
	public int getLengthInt() {
		return data.length;
	}
	
	public String getLength() {
		return Integer.toString(data.length);
	}
	
	public String getType() {
		return type;
	}
	
	public Path getPath() {
		return path;
	}
}
