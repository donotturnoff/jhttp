package net.donotturnoff.jhttp;

import java.util.HashMap;

public class Config {
	private final HashMap<String, String> config;
	private final HashMap<String, String> defaults;
	
	public Config() {
		config = new HashMap<>();
		defaults = new HashMap<>();
		defaults.put("log", "/var/log/jhttp.log");
		defaults.put("port", "80");
		defaults.put("index", "index.html,index.htm");
		defaults.put("root", "/var/www/html/");
		defaults.put("timeout", "0");
		defaults.put("symlinks", "no");
		defaults.put("defaultmime", "text/html");
		defaults.put("defaultprotocol", "HTTP/1.1");
	}
	
	public void put(String key, String value) {
		config.put(key, value);
	}
	
	public String get(String key) {
		/* Attempt to read config value.
		 * Resort to default if not found.
		 * If default doesn't exist return an empty string. */
		String value = config.get(key);
		String defaultValue = defaults.get(key);
		if (value != null) {
			return value;
		} else if (defaultValue != null) {
			return defaultValue;
		} else {
			return "";
		}
	}
	
	public HashMap<String, String> getConfig() {
		return config;
	}
}
