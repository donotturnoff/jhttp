package net.donotturnoff.jhttp;

import java.io.*;
import java.util.*;

public class PHPDocument extends Document {
	
	private final Headers phpHeaders;
	
	public PHPDocument(JHTTP server, InputStream in, InputStream err) {
		super(server);
		
		BufferedReader bin = new BufferedReader(new InputStreamReader(in));
		BufferedReader berr = new BufferedReader(new InputStreamReader(err));
		StringBuilder sb = new StringBuilder();
		String inLine, errLine;
		
		phpHeaders = new Headers();
		
		/* 
		 * Parse headers output by php-cgi.
		 * Headers are separated from body by a blank line.
		 * Split key from value at first colon.
		 */
		try {
			while (!(inLine = bin.readLine()).equals("")) {
				String[] parts = inLine.split(":");
				String key = parts[0].trim();
				String[] tail = Arrays.copyOfRange(parts, 1, parts.length);
				String value = String.join(":", tail).trim(); 
				phpHeaders.setHeader(key, value);
			}
		} catch (IOException ignored) {}
		
		/* Read all incoming data from php-cgi's output stream. */
		try {
			while ((inLine = bin.readLine()) != null) {
				sb.append(inLine).append("\n");
			}
		} catch (IOException ignored) {}
		
		/* Read all incoming data from php-cgi's error stream. */
		try {
			while ((errLine = berr.readLine()) != null) {
				sb.append(errLine).append("\n");
			}
		} catch (IOException ignored) {}
		
		data = sb.toString().getBytes();
		
		type = "text/html";
	}
	
	public Headers getPHPHeaders() {
		return phpHeaders;
	}
}
