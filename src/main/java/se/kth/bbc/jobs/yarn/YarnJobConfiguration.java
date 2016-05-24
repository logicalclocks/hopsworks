package se.kth.bbc.jobs.yarn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import se.kth.bbc.jobs.MutableJsonObject;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.configuration.JobConfiguration;
import se.kth.hopsworks.controller.LocalResourceDTO;
import se.kth.hopsworks.controller.LocalResourceDTO;
import se.kth.hopsworks.controller.LocalResourceHopsWorks;
import se.kth.hopsworks.util.Settings;

/**
 * Contains user-setable configuration parameters for a Yarn job.
 * <p/>
 * @author stig
 */
@XmlRootElement
public class YarnJobConfiguration extends JobConfiguration {

    private String amQueue = "default";
    // Memory for App master (in MB)
    private int amMemory = Settings.YARN_DEFAULT_APP_MASTER_MEMORY;
    //Number of cores for appMaster
    private int amVCores = 1;
    //List of paths to be added to local resources
    private LocalResourceDTO[] localResources = new LocalResourceDTO[0];
    protected final static String KEY_TYPE = "type";
    protected final static String KEY_QUEUE = "QUEUE";
    protected final static String KEY_AMMEM = "AMMEM";
    protected final static String KEY_AMCORS = "AMCORS";
    protected final static String KEY_RESOURCES = "RESOURCES";

    public final static String KEY_RESOURCESPATH = "RESOURCESPATH";
    public final static String KEY_RESOURCESVISIBILITY = "RESOURCESVISIBILITY";
    public final static String KEY_RESOURCESTYPE = "RESOURCESTYPE";
    public final static String KEY_RESOURCESPATTERN = "RESOURCESPATTERN";
    public final static String KEY_RESOURCESNAME = "RESOURCESNAME";

    public YarnJobConfiguration() {
        super();
    }

    public final String getAmQueue() {
        return amQueue;
    }

    /**
     * Set the queue to which the application should be submitted to the
     * ResourceManager. Default value: "".
     * <p/>
     * @param amQueue
     */
    public final void setAmQueue(String amQueue) {
        this.amQueue = amQueue;
    }

    public final int getAmMemory() {
        return amMemory;
    }

    /**
     * Set the amount of memory in MB to be allocated for the Application Master
     * container. Default value: 1024.
     * <p/>
     * @param amMemory
     */
    public final void setAmMemory(int amMemory) {
        this.amMemory = amMemory;
    }

    public final int getAmVCores() {
        return amVCores;
    }

    /**
     * Set the number of virtual cores to be allocated for the Application
     * Master container. Default value: 1.
     * <p/>
     * @param amVCores
     */
    public final void setAmVCores(int amVCores) {
        this.amVCores = amVCores;
    }

    public LocalResourceDTO[] getLocalResources() {
        return localResources;
    }

    public void setLocalResources(LocalResourceDTO[] localResources) {
        this.localResources = localResources;
    }

    @Override
    public JobType getType() {
        return JobType.YARN;
    }

    @Override
    public MutableJsonObject getReducedJsonObject() {
        MutableJsonObject obj = super.getReducedJsonObject();
        //First: fields that can be empty or null:
        if (localResources != null && localResources.length >0 ) {
            MutableJsonObject resources = new MutableJsonObject();
//      for (LocalResourceDTO dto : localResources) {
//         MutableJsonObject localResourceJson = new MutableJsonObject();
//         localResourceJson.set(KEY_RESOURCESNAME, dto.getName());
//         localResourceJson.set(KEY_RESOURCESPATH, dto.getPath());
//         localResourceJson.set(KEY_RESOURCESTYPE, dto.getType().toString());
//         localResourceJson.set(KEY_RESOURCESVISIBILITY, dto.getVisibility().toString());
//         localResourceJson.set(KEY_RESOURCESPATTERN, dto.getPattern());    
//         resources.set(dto.getName(), localResourceJson);
//     }
            obj.set(KEY_RESOURCES, resources);
        }
        //Then: fields that cannot be null or emtpy:
        obj.set(KEY_AMCORS, "" + amVCores);
        obj.set(KEY_AMMEM, "" + amMemory);
        obj.set(KEY_QUEUE, amQueue);
        obj.set(KEY_TYPE, JobType.YARN.name());
        return obj;
    }

    @Override
    public void updateFromJson(MutableJsonObject json) throws
            IllegalArgumentException {
        //First: make sure the given object is valid by getting the type and AdamCommandDTO
        JobType type;
        String jsonCors, jsonMem, jsonQueue;
        LocalResourceDTO[] jsonResources = new LocalResourceDTO[0];
        try {
            String jsonType = json.getString(KEY_TYPE);
            type = JobType.valueOf(jsonType);
            if (type != JobType.YARN) {
                throw new IllegalArgumentException("JobType must be YARN.");
            }
            //First: fields that can be null or empty:
            if (json.containsKey(KEY_RESOURCES)) {
//        jsonResources = new ArrayList<>();
//        MutableJsonObject resources = json.getJsonObject(KEY_RESOURCES);
//        for(String key:resources.keySet()){
//            MutableJsonObject resource = resources.getJsonObject(key);
//            jsonResources.add( new LocalResourceDTO(
//                    resource.getString(KEY_RESOURCESNAME),
//                    resource.getString(KEY_RESOURCESPATH), 
//                    LocalResourceVisibility.valueOf(resource.getString(KEY_RESOURCESVISIBILITY)),
//                    LocalResourceType.valueOf(resource.getString(KEY_RESOURCESTYPE)), 
//                    resource.getString(KEY_RESOURCESPATTERN)));
//        }
            }
            //Then: fields that cannot be null or empty
            jsonCors = json.getString(KEY_AMCORS);
            jsonMem = json.getString(KEY_AMMEM);
            jsonQueue = json.getString(KEY_QUEUE);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot convert object into YarnJobConfiguration.", e);
        }
        super.updateFromJson(json);
        //Second: we're now sure everything is valid: actually update the state
        this.localResources = jsonResources;
        this.amMemory = Integer.parseInt(jsonMem);
        this.amQueue = jsonQueue;
        this.amVCores = Integer.parseInt(jsonCors);
    }

}
