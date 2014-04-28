/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.upload;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import javax.servlet.RequestDispatcher;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;



/**
 *
 * @author roshan
 */

public class MyServlet extends HttpServlet{
    
    protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();

		FileItemFactory fileItemFactory = new DiskFileItemFactory();
		ServletFileUpload servletFileUpload = new ServletFileUpload(fileItemFactory);
		try {
			List<FileItem> fileItems = servletFileUpload.parseRequest(request);
			Iterator<FileItem> iterator = fileItems.iterator();
			while (iterator.hasNext()) {
				FileItem fileItem = iterator.next();
				boolean formFied = fileItem.isFormField();
				if (formFied) {
					out.println("<td>regular form field"
							+ fileItem.getFieldName());
				} else {
					String fileName = request.getRealPath("") + "/images/"+ fileItem.getName();
					OutputStream outputStream = new FileOutputStream(fileName);
					InputStream inputStream = fileItem.getInputStream();
					
					int readBytes = 0;
					byte[] buffer = new byte[10000];
					while ((readBytes = inputStream.read(buffer, 0, 10000)) != -1) {
						outputStream.write(buffer, 0, readBytes);
					}
					outputStream.close();
					inputStream.close();
					if(fileItem.getName()!= null){
						out.println("<td><img width='100' heigth='100' src="+ request.getContextPath() + "/images/"+ fileItem.getName() + "></td>");
					}					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
                
                RequestDispatcher rd = request.getRequestDispatcher("MyServlet");
                rd.forward(request, response);

	}

    @Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
//		String filePath =request.getRealPath("") + "/images/angelina_jollie.jpg";
//		String fileName ="angelina_jollie.jpg";
//
//		FileInputStream fileToDownload = new FileInputStream(filePath);
//		ServletOutputStream output = response.getOutputStream();
//		response.setContentType("image/jpeg");
//		response.setHeader("Content-Disposition", "attachment; filename="+ fileName);
//		response.setContentLength(fileToDownload.available());
//		
//		int readBytes = 0;
//		byte[] buffer = new byte[10000];
//		while ((readBytes = fileToDownload.read(buffer, 0, 10000)) != -1) {
//			output.write(readBytes);
//		}
//		
//		output.close();
//		fileToDownload.close();
//		fileToDownload.close();
//	}
	
                
}
}
