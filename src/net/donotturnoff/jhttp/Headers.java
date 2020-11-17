package net.donotturnoff.jhttp;

import java.util.HashMap;

public class Headers {
	private HashMap<String, String> headers;
	
	public Headers() {
		setHeaders(new HashMap<String, String>());
	}
	
	public Headers(HashMap<String, String> headers) {
		setHeaders(headers);
	}
	
	public void setHeader(String key, String value) {
		headers.put(key, value);
	}
	
	public void setHeaders(HashMap<String, String> headers) {
		this.headers = headers;
	}
	
	public String getHeader(String key) {
		String value = headers.get(key);
		if (value != null) {
			return value;
		} else {
			return "";
		}
	}
	
	public HashMap<String, String> getHeaders() {
		return headers;
	}
}
