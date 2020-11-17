package net.donotturnoff.jhttp;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.logging.*;

public class DirectoryListingDocument extends Document {
	
	public DirectoryListingDocument(JHTTP server, Request request, Path path) throws IOException {
		super(server);
		Host host = request.getHost();
		
		File dir = new File(host.get("root") + "/" + path.toString());
		if (!dir.exists() || !dir.isDirectory()) {
			throw new FileNotFoundException("No such directory");
		}
		if (!dir.canRead()) {
			throw new AccessDeniedException("No read privilege on " + path.toString());
		}
		File[] dirList = dir.listFiles();
		Arrays.sort(dirList);
		
		/* Build directory listing table in alphabetical order containing type (file/dir) and filename. */
		StringBuilder docBuilder = new StringBuilder();
		docBuilder.append("<!DOCTYPE html>\n");
		docBuilder.append("<html>\n");
		docBuilder.append(" <head>\n");
		docBuilder.append("  <meta charset=\"UTF-8\" />\n");
		docBuilder.append("  <style>\n");
		docBuilder.append("table {border-collapse: collapse; border: 1px solid black} th, td {border: 1px solid black; padding: 5px}\n");
		docBuilder.append("  </style>\n");
		docBuilder.append("  <title>");
		docBuilder.append(path);
		docBuilder.append("</title>\n");
		docBuilder.append(" </head>\n");
		docBuilder.append(" <body>\n");
		docBuilder.append("  <h1>Directory listing for ");
		docBuilder.append(path);
		docBuilder.append("</h1>\n");
		docBuilder.append("  <table>\n");
		docBuilder.append("   <thead>\n");
		docBuilder.append("    <tr>\n");
		docBuilder.append("     <th>Type</th>\n");
		docBuilder.append("     <th>Filename</th>\n");
		docBuilder.append("    </tr>\n");
		docBuilder.append("   </thead>\n");
		docBuilder.append("   <tbody>\n");
		
		for (File file: dirList) {
			String type = "";
			if (file.isFile()) {
				type = "file";
			} else if (file.isDirectory()) {
				type = "dir";
			}
			docBuilder.append("    <tr>\n");
			docBuilder.append("     <td>");
			docBuilder.append(type);
			docBuilder.append("</td>\n");
			docBuilder.append("     <td>\n");
			docBuilder.append("      <a href=\"");
			docBuilder.append(path.resolve(file.getName()));
			docBuilder.append("\">");
			docBuilder.append(file.getName());
			docBuilder.append("</a>\n");
			docBuilder.append("     </td>\n");
			docBuilder.append("    </tr>\n");
		}
		
		docBuilder.append("   </tbody>\n");
		docBuilder.append("  </table>\n");
		docBuilder.append(" </body>\n");
		docBuilder.append("</html>\n");
		data = docBuilder.toString().getBytes();
		
		type = "text/html";
		this.path = path;
	}
}
