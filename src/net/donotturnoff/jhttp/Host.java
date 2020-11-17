package net.donotturnoff.jhttp;

import java.util.HashMap;

public class Host {
	private String hostname;
	private Config hostConfig;
	private HashMap<String, Config> documentConfigs;
	
	public Host(String hostname, Config hostConfig, HashMap<String, Config> documentConfigs) {
		this.hostname = hostname;
		this.hostConfig = hostConfig;
		this.documentConfigs = documentConfigs;
	}
	
	public Config getHostConfig() {
		return hostConfig;
	}
	
	public String get(String key) {
		return hostConfig.get(key);
	}
	
	public String[] getIndexPages() {
		/* Parse index page list (CSV-like). */
		return hostConfig.get("index").split(",");
	}
	
	public String getHostname() {
		return hostname;
	}
	
	public Config getDocumentConfig(String path) {
		Config config = documentConfigs.get(path);
		if (config != null) {
			return config;
		} else {
			return new Config();
		}
	}
	
	public String toString() {
		String returnValue = "";
		for (HashMap.Entry<String, String> entry: hostConfig.getConfig().entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			returnValue += key + " => " + value + "\n";
		}
		
		return returnValue;
	}
}
