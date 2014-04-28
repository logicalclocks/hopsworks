/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "dataset_study")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "DatasetStudy.findAll", query = "SELECT d FROM DatasetStudy d"),
    @NamedQuery(name = "DatasetStudy.findByDatasetid", query = "SELECT d FROM DatasetStudy d WHERE d.datasetStudyPK.datasetid = :datasetid"),
    @NamedQuery(name = "DatasetStudy.findById", query = "SELECT d FROM DatasetStudy d WHERE d.datasetStudyPK.id = :id")})
    //@NamedQuery(name = "DatasetStudy.findByNameOwner", query = "SELECT t.name, ds.owner FROM DatasetStudy d, TrackStudy t, Dataset ds WHERE t.trackStudyPK.datasetId = :datasetid AND t.trackStudyPK.id = :id AND ds.id = :datasetid")})
    
public class DatasetStudy implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected DatasetStudyPK datasetStudyPK;
    
    
//    @ManyToOne
//    private List<TrackStudy> study;
//    
//    @ManyToOne    
//    private List<Dataset> dataset;

    public DatasetStudy() {
    }

    public DatasetStudy(DatasetStudyPK datasetStudyPK) {
        this.datasetStudyPK = datasetStudyPK;
    }

    public DatasetStudy(int datasetid, int id) {
        this.datasetStudyPK = new DatasetStudyPK(datasetid, id);
    }

    public DatasetStudyPK getDatasetStudyPK() {
        return datasetStudyPK;
    }

    public void setDatasetStudyPK(DatasetStudyPK datasetStudyPK) {
        this.datasetStudyPK = datasetStudyPK;
    }

    
//    public List<TrackStudy> getStudy(){
//        if(study == null) {
//            study = new ArrayList<TrackStudy>();
//        }
//        return study;
//    }
//    
//    public void setStudy(List<TrackStudy> study){
//        this.study = study;
//    }
//    
//    public List<Dataset> getDataset(){
//        if(dataset == null) {
//            dataset = new ArrayList<Dataset>();
//        }
//        return dataset;
//    }
//    
//    public void setDataset(List<Dataset> dataset){
//        this.dataset = dataset;
//    }
//    
    
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (datasetStudyPK != null ? datasetStudyPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetStudy)) {
            return false;
        }
        DatasetStudy other = (DatasetStudy) object;
        if ((this.datasetStudyPK == null && other.datasetStudyPK != null) || (this.datasetStudyPK != null && !this.datasetStudyPK.equals(other.datasetStudyPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.DatasetStudy[ datasetStudyPK=" + datasetStudyPK + " ]";
    }
    
}
