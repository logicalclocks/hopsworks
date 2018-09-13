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

package io.hops.hopsworks.admin.fsOps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import org.apache.hadoop.fs.Path;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.admin.maintenance.Utils;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;

//TODO: report errors to user!!! seems to be going wrong!
/**
 * Managed bean for accessing operations on the file system. Downloading,
 * uploading, creating files and directories. Methods do not care about the
 * specific implementation of the file system (i.e. separation between DB and FS
 * is not made here).
 * <p>
 */
//Not sure it makes sense to have it as an MB. Perhaps functionality should be 
//split between an MB and EJB
@ManagedBean(name = "fileOperationsMB")
@RequestScoped
public class FileOperationsManagedBean implements Serializable {

  @EJB
  private DistributedFsService fileOps;

  private String newFolderName;
  private static final Logger logger = Logger.getLogger(
          FileOperationsManagedBean.class.getName());

  /**
   * Download the file at the specified inode.
   *
   * @param path
   * @return StreamedContent of the file to be downloaded.
   */
  public StreamedContent downloadFile(String path) {

    StreamedContent sc = null;
    DistributedFileSystemOps dfso = null;
    InputStream is = null;
    try {
      //TODO: should convert to try-with-resources? or does that break streamedcontent?
      dfso = fileOps.getDfsOps();
      is = dfso.open(path);
      String extension = Utils.getExtension(path);
      String filename = Utils.getFileName(path);

      sc = new DefaultStreamedContent(is, extension, filename);
      logger.log(Level.FINE, "File was downloaded from HDFS path: {0}", path);
    } catch (IOException ex) {
      Logger.getLogger(FileOperationsManagedBean.class.getName()).log(
              Level.SEVERE, null, ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Download failed.");
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Error while closing stream.", ex);
        }
      }
      if (dfso != null) {
        dfso.close();
      }
    }
    return sc;
  }

  /**
   * Create a new folder with the name newFolderName (class property) at the
   * specified path. The path must NOT contain the new folder name. Set this
   * using the <i>newFolderName</i> property.
   *
   * @param path Location at which to create the new folder, not including the
   * name of the new folder.
   */
  public void mkDir(String path) {
    String location;
    DistributedFileSystemOps dfso = null;
    if (path.endsWith(File.separator)) {
      location = path + newFolderName;
    } else {
      location = path + File.separator + newFolderName;
    }
    try {
      dfso = fileOps.getDfsOps();
      boolean success = dfso.mkdir(location);
      if (success) {
        newFolderName = null;
      } else {
        MessagesController.addErrorMessage(MessagesController.ERROR,
                "Failed to create folder.");
      }
    } catch (IOException ex) {
      Logger.getLogger(FileOperationsManagedBean.class.getName()).log(
              Level.SEVERE, null, ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Failed to create folder.");
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }

  public void setNewFolderName(String s) {
    newFolderName = s;
  }

  public String getNewFolderName() {
    return newFolderName;
  }

  public void deleteFile(String path) {
    DistributedFileSystemOps dfso = null;
    Path location = new Path(path);
    try {
      dfso = fileOps.getDfsOps();
      dfso.rm(location, false);
    } catch (IOException ex) {
      Logger.getLogger(FileOperationsManagedBean.class.getName()).log(
              Level.SEVERE, "Failed to remove file.", ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Remove failed.");
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }

  public void deleteFolderRecursive(String path) {
    DistributedFileSystemOps dfso = null;
    Path location = new Path(path);
    try {
      dfso = fileOps.getDfsOps();
      dfso.rm(location, true);
    } catch (IOException ex) {
      Logger.getLogger(FileOperationsManagedBean.class.getName()).log(
              Level.SEVERE, "Failed to remove file.", ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Remove failed.");
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }
}
