
package se.kth.bbc.jobs.jobhistory;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.ejb.Stateless;

@Stateless
public class HeuristicsBean implements Serializable{
    
    private static final Logger logger = Logger.getLogger(HeuristicsBean.class.
      getName());
    
    private int vCores , memory, size;
    private String className, name, jobType;
    
    public HeuristicsBean(){}
    
    public HeuristicsBean(int vCores, int memory, String className, int size, String name){
        this.vCores = vCores;
        this.memory = memory;
        this.className = className;
        this.size = size;
        this.name = name;
    }

    /**
     * @return the vCores
     */
    public int getvCores() {
        return vCores;
    }

    /**
     * @param vCores the vCores to set
     */
    public void setvCores(int vCores) {
        this.vCores = vCores;
    }

    /**
     * @return the memory
     */
    public int getMemory() {
        return memory;
    }

    /**
     * @param memory the memory to set
     */
    public void setMemory(int memory) {
        this.memory = memory;
    }

    /**
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * @param className the className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the jobType
     */
    public String getJobType() {
        return jobType;
    }

    /**
     * @param jobType the jobType to set
     */
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
     
    
}
