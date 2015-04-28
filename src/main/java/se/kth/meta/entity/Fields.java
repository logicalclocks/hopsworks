
package se.kth.meta.entity;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Vangelis
 */
@Entity
@Table(name = "fields")
@NamedQueries({
    @NamedQuery(name = "Fields.findAll", query = "SELECT f FROM Fields f"),
    @NamedQuery(name = "Fields.findById", query = "SELECT f FROM Fields f WHERE f.id = :id"),
    @NamedQuery(name = "Fields.findByTableid", query = "SELECT f FROM Fields f WHERE f.tableid = :tableid"),
    @NamedQuery(name = "Fields.findByName", query = "SELECT f FROM Fields f WHERE f.name = :name"),
    @NamedQuery(name = "Fields.findByType", query = "SELECT f FROM Fields f WHERE f.type = :type"),
    @NamedQuery(name = "Fields.findByMaxsize", query = "SELECT f FROM Fields f WHERE f.maxsize = :maxsize"),
    @NamedQuery(name = "Fields.findBySearchable", query = "SELECT f FROM Fields f WHERE f.searchable = :searchable"),
    @NamedQuery(name = "Fields.findByRequired", query = "SELECT f FROM Fields f WHERE f.required = :required")})
public class Fields implements Serializable, EntityIntf {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Basic(optional = false)
    @Column(name = "fieldid")
    private Integer id;

    @Basic(optional = false)
    @NotNull
    @Column(name = "tableid")
    private int tableid;

    @ManyToOne(optional = false)
    @PrimaryKeyJoinColumn(name = "tableid", referencedColumnName = "tableid")
    private Tables tables;

    @OneToMany(mappedBy = "fields", targetEntity = RawData.class,
            fetch = FetchType.LAZY, cascade = {CascadeType.ALL}) 
    private List<RawData> raw;

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "name")
    private String name;

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "type")
    private String type;

    @Basic(optional = false)
    @NotNull
    @Column(name = "maxsize")
    private int maxsize;

    @Basic(optional = false)
    @NotNull
    @Column(name = "searchable")
    private short searchable;

    @Basic(optional = false)
    @NotNull
    @Column(name = "required")
    private short required;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "description")
    private String description;
    
    /*
     * indicates whether a field associated with raw data should be deleted along 
     * with its data or not
     */
    @Transient
    private boolean forceDelete;

    public Fields() {
    }

    public Fields(Integer id) {
        this.id = id;
        this.raw = new LinkedList<>();
    }

    public Fields(Integer id, int tableid, String name, String type, int maxsize, 
            short searchable, short required, String description) {
        this.id = id;
        this.tableid = tableid;
        this.name = name;
        this.type = type;
        this.maxsize = maxsize;
        this.searchable = searchable;
        this.required = required;
        this.description = description;
        this.raw = new LinkedList<>();
    }

    @Override
    public void copy(EntityIntf fields) {
        Fields f = ((Fields) fields);

        this.id = f.getId();
        this.tableid = f.getTableid();
        this.name = f.getName();
        this.type = f.getType();
        this.maxsize = f.getMaxsize();
        this.searchable = f.getSearchable() ? (short) 1 : (short) 0;
        this.required = f.getRequired() ? (short) 1 : (short) 0;
        this.raw = f.getRawData();
        this.description = f.getDescription();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public int getTableid() {
        return tableid;
    }

    public void setTableid(int tableid) {
        this.tableid = tableid;
    }
    
    /* get and set the parent entity */
    public Tables getTables(){
        return this.tables;
    }
    
    public void setTables(Tables tables){
        this.tables = tables;
    }
    /*-------------------------------*/
    
    /* get and set the child entities */
    public List<RawData> getRawData(){
        return this.raw;
    }
    
    public void setRawData(List<RawData> raw){
        this.raw = raw;
    }
    
    public void addRawData(RawData raw){
        this.raw.add(raw);
        if(raw != null){
            raw.setFields(this);
        }
    }
    
    public void removeRawData(RawData raw){
        this.raw.remove(raw);
        if(raw != null){
            raw.setFields(null);
        }
    }
    /*-------------------------------*/
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getMaxsize() {
        return maxsize;
    }

    public void setMaxsize(int maxsize) {
        this.maxsize = maxsize;
    }

    public boolean getSearchable() {
        return searchable == 1;
    }

    public void setSearchable(short searchable) {
        this.searchable = searchable;
    }

    public void setDescription(String description){
        this.description = description;
    }
    
    public String getDescription(){
        return this.description;
    }
    
    public boolean getRequired() {
        return required == 1;
    }

    public void setRequired(short required) {
        this.required = required;
    }

    public void setForceDelete(boolean delete){
        this.forceDelete = delete;
    }
    
    public boolean forceDelete(){
        return this.forceDelete;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Fields)) {
            return false;
        }
        Fields other = (Fields) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entity.Fields[ id=" + id + " ]";
    }

}
