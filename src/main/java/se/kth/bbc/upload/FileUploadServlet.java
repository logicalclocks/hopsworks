/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.upload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;


/**
 *
 * @author roshan
 */
@WebServlet(name = "FileUploadServlet", urlPatterns = {"/Fileupload"})
public class FileUploadServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    
     private static final Logger logger = Logger.getLogger(FileUploadServlet.class.getName());
     public static final int SIZE_THRESHOLD = 10240000;
    
     
     
     
     
     protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet Fileupload</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Hello World</h1>");
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }
     
     
     
     
     
     
     
     
     
     
     
     
    
//    @Override
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
//        
//        response.setContentType("text/html;charset=UTF-8");
//        PrintWriter out = response.getWriter();
//        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
//        logger.info("Content Type "+ request.getContentType());
//       
//        
//        try{
//            if(!isMultipart){
//                System.out.println("Request cannot be processed!");
//            } else {
//                
//                //ServletContext servletContext = this.getServletConfig().getServletContext();
//                //File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
//                
//                File repository = new File(System.getProperty("java.io.tmpdir"));
//                System.out.println(repository.getPath());
//                
//                DiskFileItemFactory factory = new DiskFileItemFactory(SIZE_THRESHOLD, repository);
//                ServletFileUpload upload = new ServletFileUpload(factory);
//                FileItemIterator iterator =  upload.getItemIterator(request);
//            
//                while(iterator.hasNext()){
//                
//                    FileItemStream item = iterator.next();
//                    
//                    
//                    if(item.isFormField()){
//                    
//                        //logger.info("A form field..... "+ item.getFieldName());
//                        out.println(item.getContentType());
//                        out.println(item.getFieldName());
//                        
//                    } else {
//                        
//                        
//                        //logger.info("File Uploading..... "+ item.getFieldName() + "name..."+item.getName());
//                        
//                        out.println(item.getName());
//                        out.println(item.getFieldName());
//                        
//                        InputStream stream = item.openStream();
//                        OutputStream outstream = new FileOutputStream(repository);
//                        
//                        int length = 0;
//                        byte[] buffer = new byte[10240];
//                        while((length = stream.read(buffer, 0, buffer.length)) != -1){
//                             //response.getOutputStream().write(buffer, 0, length);
//                              outstream.write(buffer, 0, length);
//                        } 
//                             //response.getOutputStream().close();
//                            outstream.close();
//                            stream.close();
//                    }
//                            
//                }
//                            
//            }   
//        } catch(Exception ex){
//                ex.printStackTrace();
//            }
//        
//         doGet(request, response);
//            
//
//        
//        
//        }
//        
//    

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
