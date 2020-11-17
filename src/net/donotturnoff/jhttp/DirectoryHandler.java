package net.donotturnoff.jhttp;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

public class DirectoryHandler extends RequestHandler {
	public DirectoryHandler(JHTTP server, Path path, Request request, boolean isSymlink) {
		super(server);
		this.request = request;
		Host host = request.getHost();
		
		RequestHandler handler = null;
		
		try {
			Path root = Paths.get(host.get("root")).toRealPath();
			Path realPath = path.toRealPath();
			
			/* 
			 * If the resource is accessed by following a symlink but the config forbids symlinks, issue a 403.
			 * Alternatively, if the resource is not accessed via a symlink but is outside of the www root, issue a 403.
			 */
			if (isSymlink) {
				if (!host.get("symlinks").equals("yes")) {
					throw new AccessDeniedException("Cannot follow symlinks");
				}
			} else if (!realPath.startsWith(root)) {
				throw new AccessDeniedException("Cannot access other parts of filesystem");
			}
			
			/*
			 * Search for each index page in the config (e.g. index.html).
			 * As soon as one of the pages is found, delegate to a FileHandler.
			 * The isSymlink argument is set if either the directory or the index page has been accessed via a symlink.
			 */
			String[] indexPages = host.getIndexPages();
			boolean indexPageFound = false;
			for (String indexPage: indexPages) {
				Path indexPath = realPath.resolve(Paths.get(indexPage));
				if (Files.exists(indexPath)) {
					handler = new FileHandler(server, indexPath, request, Files.isSymbolicLink(indexPath) || isSymlink);
					indexPageFound = true;
					break;
				}
			}
			
			/* 
			 * If no index page has been found, attempt to produce a directory listing. 
			 * Directory listing may be disabled in the config - in that case, produce a 403.
			 */
			if (!indexPageFound) {
				status = new Status("200");
				try {
					if (host.get("directorylisting").equals("yes")) {
						document = new DirectoryListingDocument(server, request, Paths.get(request.getPath()));
						headers = new Headers();
						headers.setHeader("Content-Type", document.getType());
					} else {
						throw new AccessDeniedException("Directory listing forbidden");
					}
				} catch (AccessDeniedException e) {
					handler = new ErrorHandler(server, request, new Status("403"));
				} catch (NoSuchFileException | FileNotFoundException e) {
					handler = new ErrorHandler(server, request, new Status("404"));
				} catch (IOException e) {
					handler = new ErrorHandler(server, request, new Status("500"), "Could not generate directory listing: " + e.getMessage());
				}
			}
		} catch (AccessDeniedException e) {
			handler = new ErrorHandler(server, request, new Status("403"), e.getMessage());
		} catch (NoSuchFileException | FileNotFoundException e) {
			handler = new ErrorHandler(server, request, new Status("404"));
		} catch (IOException e) {
			handler = new ErrorHandler(server, request, new Status("500"), "Could not generate directory listing: " + e.getMessage());
		} finally {
			if (handler != null) {
				status = handler.getStatus();
				headers = handler.getHeaders();
				document = handler.getDocument();
			}
		}
	}
}
