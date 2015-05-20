/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.controller;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.rest.AppException;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DataSetController {

  private final static Logger logger = Logger.getLogger(DataSetController.class.
          getName());
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private FileOperations fileOps;

  /**
   * Returns all the DataSet in a project
   *
   * @throws AppException
   */
  public void getDataSets() throws AppException {

  }

  /**
   * returns a DataSet
   *
   * @throws AppException
   */
  public void getDataSet() throws AppException {

  }

  /**
   * creates a new DataSet in a project
   *
   * @param project
   * @param dataSetName
   * @param owner
   * @throws AppException
   * @throws IOException
   */
  public void newDataSet(Project project, String dataSetName, String owner)
          throws AppException, IOException {
    //do some validation first
    String rootDir = Constants.DIR_ROOT;
    String projectPath = File.separator + rootDir + File.separator + project.
            getName();
    String dataSetPath = projectPath + File.separator
            + Constants.DIR_DATASET + File.separator + dataSetName;
    fileOps.mkDir(dataSetPath, -1);
  }

  public void uploadFileToFolder(Project project, String dataSetName) {
    String rootDir = Constants.DIR_ROOT;
    String projectPath = File.separator + rootDir + File.separator + project.
            getName();
    String dataSetPath = projectPath + File.separator
            + Constants.DIR_DATASET + File.separator + dataSetName;
    fileOps.startUpload(dataSetPath);
  }
  
  public void deleteFile(String path) throws IOException{
    try {
      fileOps.rm(path);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Failed to remove file.", ex);     
    }
  }

  public void deleteFolderRecursive(String path) {
    try {
      fileOps.rmRecursive(path);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Failed to remove file.", ex);
    }
  }

}
