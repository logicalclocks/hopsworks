package se.kth.bbc.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.StagingManager;
import se.kth.bbc.study.fb.InodesMB;

/**
 * by fanxu
 */
public class UploadServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(UploadServlet.class.
          getName());
  @EJB
  private FileOperations fileOps;

  @EJB
  private StagingManager stagingManager;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    InodesMB inodes = (InodesMB) request.getSession().getAttribute("InodesMB");
    //Here because current path may change while uploading
    String uploadPath = inodes.getCwdPath();

    int resumableChunkNumber = getResumableChunkNumber(request);

    ResumableInfo info = getResumableInfo(request);

    long content_length;
    //Seek to position
    try (RandomAccessFile raf
            = new RandomAccessFile(info.resumableFilePath, "rw");
            InputStream is = request.getInputStream()) {
      //Seek to position
      raf.seek((resumableChunkNumber - 1) * (long) info.resumableChunkSize);
      //add an entry to the Inodes database: file is uploading.
      if (resumableChunkNumber == 1) {
        fileOps.startUpload(uploadPath + info.resumableFilename);
      }
      //Save to file
      long readed = 0;
      content_length = request.getContentLength();
      byte[] bytes = new byte[1024 * 100];
      while (readed < content_length) {
        int r = is.read(bytes);
        if (r < 0) {
          break;
        }
        raf.write(bytes, 0, r);
        readed += r;
      }
    }

    boolean finished = false;

    //Mark as uploaded and check if finished
    try (PrintWriter c = response.getWriter()) {
      if (info.addChuckAndCheckIfFinished(
              new ResumableInfo.ResumableChunkNumber(
                      resumableChunkNumber), content_length)) { //Check if all chunks uploaded, and change filename
        ResumableInfoStorage.getInstance().remove(info);
        c.print("All finished.");
        finished = true;
      } else {
        c.print("Upload");
      }
    }

    if (finished) {
      try {
        fileOps.copyAfterUploading(info.resumableFilename, uploadPath
                + info.resumableFilename);
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to write to HDSF", e);
      }
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    int resumableChunkNumber = getResumableChunkNumber(request);

    ResumableInfo info = getResumableInfo(request);
    try (PrintWriter c = response.getWriter()) {
      if (info.isUploaded(new ResumableInfo.ResumableChunkNumber(
              resumableChunkNumber))) {
        c.print("Uploaded."); //This Chunk has been Uploaded.
      } else {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
    }
  }

  private int getResumableChunkNumber(HttpServletRequest request) {
    return HttpUtils.toInt(request.getParameter("resumableChunkNumber"), -1);
  }

  private ResumableInfo getResumableInfo(HttpServletRequest request) throws
          ServletException {
    String base_dir = stagingManager.getStagingPath();

    int resumableChunkSize = HttpUtils.toInt(request.getParameter(
            "resumableChunkSize"), -1);
    long resumableTotalSize = HttpUtils.toLong(request.getParameter(
            "resumableTotalSize"), -1);
    String resumableIdentifier = request.getParameter("resumableIdentifier");
    String resumableFilename = request.getParameter("resumableFilename");
    String resumableRelativePath = request.getParameter("resumableRelativePath");
    //Here we add a ".temp" to every upload file to indicate NON-FINISHED
    String resumableFilePath = new File(base_dir, resumableFilename).
            getAbsolutePath() + ".temp";
    //TODO: the current way of uploading will not scale: if two persons happen to upload a file with the same name, trouble is waiting to happen

    ResumableInfoStorage storage = ResumableInfoStorage.getInstance();

    ResumableInfo info = storage.get(resumableChunkSize, resumableTotalSize,
            resumableIdentifier, resumableFilename, resumableRelativePath,
            resumableFilePath);
    if (!info.vaild()) {
      storage.remove(info);
      throw new ServletException("Invalid request params.");
    }
    return info;
  }

}
