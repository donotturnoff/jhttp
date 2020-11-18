package net.donotturnoff.jhttp;

import java.util.HashMap;

public class Status {
	
	private static final HashMap<String, String> codes = new HashMap<>();
	private static final HashMap<String, String> groups = new HashMap<>();
	
	static {
		codes.put("100", "Continue");
		codes.put("101", "Switching Protocol");
		codes.put("102", "Processing");
		codes.put("200", "OK");
		codes.put("201", "Created");
		codes.put("202", "Accepted");
		codes.put("203", "Non-Authoritative Information");
		codes.put("204", "No Content");
		codes.put("205", "Reset Content");
		codes.put("206", "Partial Content");
		codes.put("207", "Multi-Status");
		codes.put("208", "Multi-Status");
		codes.put("226", "IM Used");
		codes.put("300", "Multiple Choice");
		codes.put("301", "Moved Permanently");
		codes.put("302", "Found");
		codes.put("303", "See Other");
		codes.put("304", "Not Modified");
		codes.put("305", "Use Proxy");
		codes.put("306", "unused");
		codes.put("307", "Temporary Redirect");
		codes.put("308", "Permanent Redirect");
		codes.put("400", "Bad Request");
		codes.put("401", "Unauthorized");
		codes.put("402", "Payment Required");
		codes.put("403", "Forbidden");
		codes.put("404", "Not Found");
		codes.put("405", "Method Not Allowed");
		codes.put("406", "Not Acceptable");
		codes.put("407", "Proxy Authentication Required");
		codes.put("408", "Request Timeout");
		codes.put("409", "Conflict");
		codes.put("410", "Gone");
		codes.put("411", "Length Required");
		codes.put("412", "Precondition Failed");
		codes.put("413", "Payload Too Large");
		codes.put("414", "URI Too Long");
		codes.put("415", "Unsupported Media Type");
		codes.put("416", "Requested Range Not Satisfiable");
		codes.put("417", "Expectation Failed");
		codes.put("418", "I'm a teapot");
		codes.put("421", "Misdirected Request");
		codes.put("422", "Unprocessable Entity");
		codes.put("423", "Locked");
		codes.put("424", "Failed Dependency");
		codes.put("425", "Too Early");
		codes.put("426", "Upgrade Required");
		codes.put("428", "Precondition Required");
		codes.put("429", "Too Many Requests");
		codes.put("431", "Request Header Fields Too Large");
		codes.put("451", "Unavailable For Legal Reasons");
		codes.put("500", "Internal Server Error");
		codes.put("501", "Not Implemented");
		codes.put("502", "Bad Gateway");
		codes.put("503", "Service Unavailable");
		codes.put("504", "Gateway Timeout");
		codes.put("505", "HTTP Version Not Supported");
		codes.put("506", "Variant Also Negotiates");
		codes.put("507", "Insufficient Storage");
		codes.put("508", "Loop Detected");
		codes.put("510", "Not Extended");
		codes.put("511", "Network Authentication Required");
		
		groups.put("1", "Information");
		groups.put("2", "Successful");
		groups.put("3", "Redirection");
		groups.put("4", "Client error");
		groups.put("5", "Server error");
	}
	
	public static boolean isCode(String code) {
		return (codes.get(code) != null);
	}
	
	public static boolean isError(String code) {
		char initial = code.charAt(0);
		return ((initial == '4' || initial == '5') && isCode(code));
	}
	
	public static String getMessage(String code) throws IllegalArgumentException {
		if (isCode(code)) {
			return codes.get(code);
		} else {
			throw new IllegalArgumentException("Invalid code");
		}
	}
	
	public static String getGroup(String code) throws IllegalArgumentException {
		/* Determine status code group by initial digit. */
		if (isCode(code)) {
			/*
			 * substring used instead of charAt because the keys are Strings.
			 * They cannot be primitive chars so I'd have to fiddle about with Chars, so I reckoned this way was simpler.
			 */
			String initial = code.substring(0, 1);
			return groups.get(initial);
		} else {
			throw new IllegalArgumentException("Invalid code");
		}
	}
	
	private final String code;
	private final String message;
	
	public Status(String code) throws IllegalArgumentException {
		if (isCode(code)) {
			this.code = code;
			this.message = getMessage(code);
		} else {
			throw new IllegalArgumentException("Invalid code");
		}
	}
	
	public String getStatus() {
		return code + " " + message;
	}
	
	public byte[] getBytes() {
		return (code + " " + message).getBytes();
	}
	
	public String getCode() {
		return code;
	}
	
	public String getMessage() {
		return message;
	}
	
	public boolean isError() {
		return isError(code);
	}
	
	public String getGroup() throws IllegalArgumentException {
		return getGroup(code);
	}
}
