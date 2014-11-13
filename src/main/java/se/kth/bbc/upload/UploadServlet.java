package se.kth.bbc.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.study.fb.InodesMB;

/**
 * by fanxu
 */
public class UploadServlet extends HttpServlet {

    public static final String UPLOAD_DIR = Constants.UPLOAD_DIR;

    @EJB
    private FileOperations fileOps;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        InodesMB inodes = (InodesMB) request.getSession().getAttribute("InodesMB");
        //Here because current path may change while uploading
        String uploadPath = inodes.getCwdPath();

        int resumableChunkNumber = getResumableChunkNumber(request);

        ResumableInfo info = getResumableInfo(request);

        RandomAccessFile raf = new RandomAccessFile(info.resumableFilePath, "rw");
        //Seek to position
        raf.seek((resumableChunkNumber - 1) * (long) info.resumableChunkSize);

        //add an entry to the Inodes database: file is uploading.
        if (resumableChunkNumber == 1) {
            fileOps.startUpload(uploadPath + info.resumableFilename);
        }

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

        boolean finished = false;

        //Mark as uploaded and check if finished
        if (info.addChuckAndCheckIfFinished(new ResumableInfo.ResumableChunkNumber(resumableChunkNumber))) { //Check if all chunks uploaded, and change filename
            ResumableInfoStorage.getInstance().remove(info);
            response.getWriter().print("All finished.");
            finished = true;
        } else {
            response.getWriter().print("Upload");
        }

        if (finished) {
            int extPos = info.resumableFilename.lastIndexOf(".");
            if (extPos == -1) {
                return;
            }
            String fileType = info.resumableFilename.substring(extPos + 1);

            fileOps.copyAfterUploading(info.resumableFilename, uploadPath + info.resumableFilename);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int resumableChunkNumber = getResumableChunkNumber(request);

        ResumableInfo info = getResumableInfo(request);

        if (info.isUploaded(new ResumableInfo.ResumableChunkNumber(resumableChunkNumber))) {
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
        //TODO: the current way of uploading will not scale: if two persons happen to upload a file with the same name, trouble is waiting to happen

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
