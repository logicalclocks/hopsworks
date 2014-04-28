/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.upload;

import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 *
 * @author roshan
 */
public class Download extends HttpServlet {
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath =request.getRealPath("") + "/images/angelina_jollie.jpg";
		String fileName ="angelina_jollie.jpg";

		FileInputStream fileToDownload = new FileInputStream(filePath);
		ServletOutputStream output = response.getOutputStream();
		response.setContentType("image/jpeg");
		response.setHeader("Content-Disposition", "attachment; filename="+ fileName);
		response.setContentLength(fileToDownload.available());
		
		int readBytes = 0;
		byte[] buffer = new byte[10000];
		while ((readBytes = fileToDownload.read(buffer, 0, 10000)) != -1) {
			output.write(readBytes);
		}
		
		output.close();
		fileToDownload.close();
		fileToDownload.close();
	}
}
