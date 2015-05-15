/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.controller;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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

    /**
     * Returns all the DataSet in a project
     * @throws AppException 
     */
    public void getDataSets() throws AppException {

    }

    /**
     * returns a DataSet 
     * @throws AppException 
     */
    public void getDataSet() throws AppException {

    }

    /**
     * creates a new DataSet in a project
     * @param project
     * @param dataSetName
     * @param owner
     * @throws AppException 
     */
    public void newDataSet(Project project, String dataSetName, String owner) throws AppException {

    }

    /**
     * deletes a DataSet 
     * @param project
     * @param dataSetName
     * @param owner
     * @throws AppException 
     */
    public void deletDataSet(Project project, String dataSetName, String owner) throws AppException {

    }

}
