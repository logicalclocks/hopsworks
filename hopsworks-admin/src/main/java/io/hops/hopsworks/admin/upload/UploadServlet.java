/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.admin.upload;

import io.hops.hopsworks.admin.project.InodesMB;
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
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.upload.HttpUtils;
import io.hops.hopsworks.common.upload.ResumableInfo;
import io.hops.hopsworks.common.upload.ResumableInfoStorage;
import io.hops.hopsworks.common.upload.StagingManager;

public class UploadServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(UploadServlet.class.
          getName());
  @EJB
  private DistributedFsService fileOps;

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
            = new RandomAccessFile(info.getResumableFilePath(), "rw");
            InputStream is = request.getInputStream()) {
      //Seek to position
      raf.seek((resumableChunkNumber - 1) * (long) info.getResumableChunkSize());
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
      if (info.addChunkAndCheckIfFinished(
              new ResumableInfo.ResumableChunkNumber(
                      //Check if all chunks uploaded, and change filename
                      resumableChunkNumber), content_length)) {
        ResumableInfoStorage.getInstance().remove(info);
        c.print("All finished.");
        finished = true;
      } else {
        c.print("Upload");
      }
    }
    DistributedFileSystemOps dfso = null;
    if (finished) {
      try {
        dfso = fileOps.getDfsOps();
        uploadPath = Utils.ensurePathEndsInSlash(uploadPath);
        dfso.copyToHDFSFromLocal(true, new File(stagingManager.
                getStagingPath(), info.getResumableFilename()).
                getAbsolutePath(), uploadPath
                + info.getResumableFilename());
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to write to HDSF", e);
      } finally {
        if (dfso != null) {
          dfso.close();
        }
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
    //TODO: the current way of uploading will not scale: if two persons happen to 
    //upload a file with the same name, trouble is waiting to happen

    ResumableInfoStorage storage = ResumableInfoStorage.getInstance();

    ResumableInfo info = storage.get(resumableChunkSize, resumableTotalSize,
            resumableIdentifier, resumableFilename, resumableRelativePath,
            resumableFilePath, -1);
    if (!info.valid()) {
      storage.remove(info);
      throw new ServletException("Invalid request params.");
    }
    return info;
  }

}
