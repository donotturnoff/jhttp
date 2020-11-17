package net.donotturnoff.jhttp;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

public class ResourceHandler extends RequestHandler {
	public ResourceHandler(JHTTP server, Request request) {
		super(server);
		this.request = request;
		Host host = request.getHost();
		Config docConfig = host.getDocumentConfig(request.getPath());
		
		RequestHandler handler = null;
		boolean authNeeded = false;
		try {
			if (docConfig.get("authType").equals("Basic")) {
				authNeeded = true;
				String[] authorisation = request.getHeader("Authorization").split(" ");
				if (authorisation[0].equals("Basic")) {
					if (authorisation.length == 2) {
						String credentials = new String(Base64.getDecoder().decode(authorisation[1])); // To do: handle error where no credentials are supplied
						authNeeded = !authenticate(docConfig.get("authFile"), credentials);
					}
				}
			}
			
			/* If authorisation is needed and hasn't been supplied yet, produce a 401 error page. */
			if (authNeeded) {
				handler = new ErrorHandler(server, request, new Status("401"), "HTTP Basic challenge");
			} else {
				
				/* 
				 * Produce the absolute path of the requested resource. 
				 * Symlinks aren't followed because they are indicated by an explicit flag which needs the original file, not the linked-to file, in order to be set.
				 */
				Path path = Paths.get(host.get("root") + request.getPath()).toRealPath(LinkOption.NOFOLLOW_LINKS);
				boolean isSymlink = Files.isSymbolicLink(path);
				
				/* 
				 * Handle resource as either directory or file.
				 * Symlinks are indicated by the isSymlink flag and are handled in the respective Handler.
				 */
				if (Files.isDirectory(path.toRealPath())) { // To do: Remove toRealPath?
					handler = new DirectoryHandler(server, path, request, isSymlink);
				} else {
					handler = new FileHandler(server, path, request, isSymlink);
				}
			}
		} catch (NoSuchFileException | FileNotFoundException e) {
			handler = new ErrorHandler(server, request, new Status("404"), e.getMessage());
		} catch (AccessDeniedException e) {
			handler = new ErrorHandler(server, request, new Status("403"), e.getMessage());
		} catch (IOException e) {
			handler = new ErrorHandler(server, request, new Status("500"), "An IO error occurred while reading " + request.getPath());
		} finally {
			status = handler.getStatus();
			headers = handler.getHeaders();
			document = handler.getDocument();
			/* If authorisation is needed and hasn't been supplied yet, set the WWW-Authenticate header to request authentication. */
			if (authNeeded) {
				headers.setHeader("WWW-Authenticate", docConfig.get("authType") + " realm=" + docConfig.get("authRealm"));
			}
		}
	}
	
	private boolean authenticate(String path, String credentials) throws IOException, FileNotFoundException {
		/* Searches for given credentials in credentials file. */
		BufferedReader authReader = new BufferedReader(new FileReader(new File(path)));
		String line;
		while ((line = authReader.readLine()) != null) {
			if (line.equals(credentials)) {
				return true;
			}
		}
		return false;
	}
}
