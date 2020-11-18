package net.donotturnoff.jhttp;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PHPHandler extends RequestHandler {
	
	public final static String[] mimes = {"text/php", "text/x-php", "application/php", "application/x-php", "application/x-httpd-php", "application/x-httpd-php-source"};
	
	private static String extractExtension(Path path) {
		String fileName = path.toString();
		String extension = "";
		int i = fileName.lastIndexOf('.');
		int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
		if (i > p && i < (fileName.length()-1)) {
			extension = fileName.substring(i+1);
		}
		return extension;
	}
	
	public static boolean isPHP(Path path) throws IOException {
		File file = new File(path.toString());
		String type = Files.probeContentType(path);
		if (type == null) {
			type = URLConnection.guessContentTypeFromName(file.getName());
		}
		if (type == null) {
			InputStream is = new BufferedInputStream(new FileInputStream(file));
			type = URLConnection.guessContentTypeFromStream(is);
		}
		String extension = extractExtension(path);
		return Arrays.asList(mimes).contains(type) || extension.equals("php");
	}
	
	public PHPHandler(JHTTP server, Request request) {
		super(server);
		this.request = request;
		Host host = request.getHost();
		
		try {
			String pathString = host.get("root") + request.getPath();
			Path path = Paths.get(pathString).toRealPath();
			ProcessBuilder builder = new ProcessBuilder();
			builder.directory(new File(System.getProperty("user.home")));
			Map<String, String> env = builder.environment();
			
			/* Build the php-cgi environment. */
			env.put("REDIRECT_STATUS", "200");
			env.put("REQUEST_METHOD", request.getVerb());
			env.put("SCRIPT_NAME", request.getPath());
			env.put("SCRIPT_FILENAME", path.toString());
			env.put("QUERY_STRING", request.getQueryString());
			env.put("SERVER_SOFTWARE", JHTTP.server);
			env.put("SERVER_PORT", cfg.get("port"));
			env.put("GATEWAY_INTERFACE", "CGI/1.1");
			env.put("CONTENT_LENGTH", Integer.toString(request.getBody().length()));
			env.put("CONTENT_TYPE", request.getHeader("Content-Type"));
			env.put("SERVER_PROTOCOL", request.getProtocol());
			env.put("HOST_NAME", request.getHeader("Host"));
			env.put("REMOTE_ADDR", request.getConnection().getHost());
			
			builder.command("php-cgi", path.toString());
			Process process = builder.start();
			
			/* Output the request body to php-cgi. */
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
			out.write(request.getBody(), 0, request.getBody().length());
			out.flush();
			
			/* Handle the output of php-cgi using a PHPDocument. */
			document = new PHPDocument(server, process.getInputStream(), process.getErrorStream());
			
			/* If exit code isn't 0, the process failed.
			 * Details of the error will be handled by the PHPDocument. */
			int exitCode = process.waitFor();
			if (exitCode == 0) {
				status = new Status("200");
			} else {
				throw new IOException("Execution failed");
			}
			headers = ((PHPDocument) document).getPHPHeaders();
			headers.setHeader("Content-Length", document.getLength());
		} catch (IOException e) {
			ErrorHandler handler = new ErrorHandler(server, request, new Status("500"), "Could not read PHP file: " + e.toString());
			status = handler.getStatus();
			headers = handler.getHeaders();
			document = handler.getDocument();
		} catch (InterruptedException e) {
			ErrorHandler handler = new ErrorHandler(server, request, new Status("500"), "PHP interpretation interrupted");
			status = handler.getStatus();
			headers = handler.getHeaders();
			document = handler.getDocument();
		}
	}
}
