package net.donotturnoff.jhttp;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import java.io.*;
import java.util.HashMap;
import javax.naming.ConfigurationException;

public class ConfigHandler {
	
	private Config serverConfig;
	private HashMap<String, Host> hosts;
	
	public ConfigHandler(String path) throws ConfigurationException, ParserConfigurationException, SAXException, IOException {
		serverConfig = new Config();
		hosts = new HashMap<>();
		
		File configFile = new File(path);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		org.w3c.dom.Document doc = dBuilder.parse(configFile);
		
		doc.getDocumentElement().normalize();

		Node root = doc.getDocumentElement();
		String rootName = root.getNodeName();
		if (rootName.equals("server")) {
			NodeList nodes = root.getChildNodes();

			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
						
				if (node.getNodeType() == Node.ELEMENT_NODE) {

					Element element = (Element) node;

					if (element.getTagName().equals("host")) {
						Host host = handleHost(node);
						hosts.put(host.getHostname(), host);
					} else {
						serverConfig.put(element.getTagName(), element.getTextContent());
					}
				
				}
			}
		} else {
			throw new ConfigurationException("Root element must be server");
		}
		
		if (hosts.size() == 0) {
			throw new ConfigurationException("No hosts specified");
		}
		if (serverConfig.get("defaultHost").equals("")) {
			throw new ConfigurationException("No default host specified");
		}
		
		for (Host host: hosts.values()) {
			if (host.get("root").equals("")) {
				throw new ConfigurationException("No www root specified for " + host.getHostname());
			}
		}
	}
	
	private int getChildElementCount(Node node) {
		int count = 0;
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
				count++;
			}
		}
		return count;
	}
	
	private Host handleHost(Node hostNode) throws ConfigurationException {
		Config hostConfig = new Config();
		HashMap<String, Config> documentConfigs = new HashMap<String, Config>();
		String headers = "";
		NodeList nodes = hostNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				
				String name = element.getTagName();
				if (name.equals("document")) {
					String path = element.getElementsByTagName("path").item(0).getTextContent();
					documentConfigs.put(path, handleDocument(node));
				} else if (name.equals("index")) {
					hostConfig.put(element.getTagName(), handleIndex(node));
				} else if (name.equals("error")) {
					String code = element.getElementsByTagName("code").item(0).getTextContent();
					String path = element.getElementsByTagName("document").item(0).getTextContent();
					hostConfig.put(code, path);
				} else if (name == "header") {
					headers += handleHeader(node);
				}  else {
					if (name.equals("default") && element.getTextContent().equals("yes")) {
						String hostname = ((Element) hostNode).getElementsByTagName("hostname").item(0).getTextContent();
						serverConfig.put("defaultHost", hostname);
					}
					hostConfig.put(element.getTagName(), element.getTextContent());
				}
				
			}
		}
		
		String hostname = hostConfig.get("hostname");
		hostConfig.put("headers", headers);
		return new Host(hostname, hostConfig, documentConfigs);
	}
	
	private String handleIndex(Node indexNode) throws ConfigurationException {
		String[] indices = new String[getChildElementCount(indexNode)];
		
		NodeList nodes = indexNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				
				if (element.getTagName().equals("document")) {
					int priority = Integer.parseInt(element.getAttributes().getNamedItem("priority").getNodeValue()) - 1;
					if (priority < indices.length && priority >= 0) {
						indices[priority] = element.getTextContent();
					} else {
						throw new ConfigurationException("Index page priorities must be consecutive positive integers starting from 1");
					}
				}
			}
		}
		
		return String.join(",", indices);
	}
	
	private Config handleDocument(Node documentNode) {
		NodeList nodes = documentNode.getChildNodes();
		Config config = new Config();
		String headers = "";
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				
				String name = element.getTagName();
				if (name == "header") {
					headers += handleHeader(node);
				} else if (name == "auth") {
					config.put("authType", "Basic");
					config.put("authRealm", "");
					config.put("authFile", "");
					config = handleAuth(node, config);
				} else {
					config.put(element.getTagName(), element.getTextContent());
				}
			}
		}
		config.put("headers", headers);
		return config;
	}
	
	private String handleHeader(Node headerNode) {
		NodeList nodes = headerNode.getChildNodes();
		String type = "";
		String content = "";
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				
				if (element.getTagName().equals("type")) {
					type = element.getTextContent();
				} else if (element.getTagName().equals("content")) {
					content = element.getTextContent();
				}
			}
		}
		return type + ":" + content + "\r\n";
	}
	
	private Config handleAuth(Node authNode, Config existingConfig) {
		NodeList nodes = authNode.getChildNodes();
		
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				
				if (element.getTagName().equals("type")) {
					existingConfig.put("authType", element.getTextContent());
				} else if (element.getTagName().equals("realm")) {
					existingConfig.put("authRealm", element.getTextContent());
				} else if (element.getTagName().equals("file")) {
					existingConfig.put("authFile", element.getTextContent());
				}
			}
		}
		return existingConfig;
	}
	
	public Config getServerConfig() {
		return serverConfig;
	}
	
	public HashMap<String, Host> getHosts() {
		return hosts;
	}
}