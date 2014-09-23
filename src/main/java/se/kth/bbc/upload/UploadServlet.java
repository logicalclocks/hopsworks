/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import se.kth.bbc.study.DatasetMB;
import se.kth.bbc.study.StudyMB;

/**
 * by fanxu
 */
public class UploadServlet extends HttpServlet {

    public static final String UPLOAD_DIR = "/tmp/upload";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int resumableChunkNumber = getResumableChunkNumber(request);

//        String sampleId = request.getParameter("sampleId");
//        String projectName = request.getParameter("projectName");
        //String fileNameBR = request.getParameter("resumableBrowse");
        //System.out.println("Browse "+fileNameBR);
//        int extPos = fileName.lastIndexOf(".");
//        if (extPos == -1) {
//            // return error to client.
//        }
//        String fileType = fileName.substring(extPos);
        // 1. Add projectName,sampleId to SampleIds EntityBean. Already exists => no error.
        // 2. create the directory in HDFS: /projects/projectName/sampleId  Already exists => no error.
        // 3. Add projectName,sampleId, fileType, fileName to SampleFiles EntityBean. Already exists => no error.
        ResumableInfo info = getResumableInfo(request);

        RandomAccessFile raf = new RandomAccessFile(info.resumableFilePath, "rw");
        //Seek to position
        raf.seek((resumableChunkNumber - 1) * (long) info.resumableChunkSize);

        //Save to file
        InputStream is = request.getInputStream();
        long readed = 0;
        long content_length = request.getContentLength();
        byte[] bytes = new byte[1024 * 100];
        while (readed < content_length) {
            int r = is.read(bytes);
            if (r < 0) {
                break;
            }
            raf.write(bytes, 0, r);
            readed += r;
        }
        raf.close();

        //Mark as uploaded.
        info.uploadedChunks.add(new ResumableInfo.ResumableChunkNumber(resumableChunkNumber));
        if (info.checkIfUploadFinished()) { //Check if all chunks uploaded, and change filename
            ResumableInfoStorage.getInstance().remove(info);
            response.getWriter().print("All finished.");
//            String studyPage = request.getContextPath() + "/bbc/lims/studyPage.xhtml";
//            System.out.println("from servlet " + studyPage);
//            response.sendRedirect(studyPage);
        } else {
            response.getWriter().print("Upload");
        }
        
        //Invoke DatasetMB in order to copy data to HDFS
        //DatasetMB dataMB = (DatasetMB) request.getSession().getAttribute("datasetMBean");
        DatasetMB dataMB = (DatasetMB) request.getAttribute(StudyMB.PROP_DATASETMB);
        
       try{
            dataMB.createDataset();
        } catch(URISyntaxException uri){
            System.out.println("uri error "+ uri.getMessage());
       }
        
        
        
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int resumableChunkNumber = getResumableChunkNumber(request);

        ResumableInfo info = getResumableInfo(request);

        if (info.uploadedChunks.contains(new ResumableInfo.ResumableChunkNumber(resumableChunkNumber))) {
            response.getWriter().print("Uploaded."); //This Chunk has been Uploaded.
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private int getResumableChunkNumber(HttpServletRequest request) {
        return HttpUtils.toInt(request.getParameter("resumableChunkNumber"), -1);
    }

    private ResumableInfo getResumableInfo(HttpServletRequest request) throws ServletException {
        String base_dir = UPLOAD_DIR;

        int resumableChunkSize = HttpUtils.toInt(request.getParameter("resumableChunkSize"), -1);
        long resumableTotalSize = HttpUtils.toLong(request.getParameter("resumableTotalSize"), -1);
        String resumableIdentifier = request.getParameter("resumableIdentifier");
        String resumableFilename = request.getParameter("resumableFilename");
        String resumableRelativePath = request.getParameter("resumableRelativePath");
        //Here we add a ".temp" to every upload file to indicate NON-FINISHED
        String resumableFilePath = new File(base_dir, resumableFilename).getAbsolutePath() + ".temp";

        ResumableInfoStorage storage = ResumableInfoStorage.getInstance();

        ResumableInfo info = storage.get(resumableChunkSize, resumableTotalSize,
                resumableIdentifier, resumableFilename, resumableRelativePath, resumableFilePath);
        if (!info.vaild()) {
            storage.remove(info);
            throw new ServletException("Invalid request params.");
        }
        return info;
    }

}
