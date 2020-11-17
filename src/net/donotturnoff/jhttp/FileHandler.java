package net.donotturnoff.jhttp;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

public class FileHandler extends RequestHandler {
	public FileHandler(JHTTP server, Path path, Request request, boolean isSymlink) {
		super(server);
		this.request = request;
		Host host = request.getHost();
		
		RequestHandler handler = null;
		
		try {
			File file = new File(path.toString());
			
			/* If the file exists but cannot be read, issue a 403. */
			boolean exists = file.exists();
			boolean readable = file.canRead();
			if (exists && !readable) {
				throw new AccessDeniedException("No read privilege");
			}
			
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
			} else if (!path.startsWith(root)) {
				throw new AccessDeniedException("Cannot access other parts of filesystem");
			}
			
			/*
			 * If the resource is a PHP document, delegate to a PHP handler.
			 * Otherwise, produce the status, document and some headers.
			 */
			if (PHPHandler.isPHP(realPath)) {
				handler = new PHPHandler(server, request);
				status = handler.getStatus();
				headers = handler.getHeaders();
				document = handler.getDocument();
			} else {
				status = new Status("200");
				document = new Document(server, realPath);
				headers = new Headers();
				headers.setHeader("Content-Type", document.getType());
				headers.setHeader("Content-Length", document.getLength());
			}
		} catch (NoSuchFileException | FileNotFoundException e) {
			handler = new ErrorHandler(server, request, new Status("404"), e.getMessage());
		} catch (AccessDeniedException e) {
			handler = new ErrorHandler(server, request, new Status("403"), e.getMessage());
		} catch (IOException e) {
			handler = new ErrorHandler(server, request, new Status("500"), "IO error: " + e.getMessage());
		} finally {
			if (handler != null) {
				status = handler.getStatus();
				headers = handler.getHeaders();
				document = handler.getDocument();
			}
		}
	}
}
