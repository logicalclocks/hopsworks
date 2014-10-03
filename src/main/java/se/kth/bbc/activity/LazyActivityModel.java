package se.kth.bbc.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

/**
 * Model for lazily loading activity data into index page
 *
 * @author stig
 */
public class LazyActivityModel extends LazyDataModel<ActivityDetail> implements Serializable {

    private transient final ActivityController activityController;
    private List<ActivityDetail> data;
    private String filterStudy;

    public LazyActivityModel(ActivityController ac) {
        this(ac, null);
    }

    public LazyActivityModel(ActivityController ac, String filterStudy) {
        super();
        this.activityController = ac;
        this.filterStudy = filterStudy;
    }

    @Override
    public List<ActivityDetail> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        System.out.println("Load called: "+first+", "+pageSize);
        List<ActivityDetail> data;
        if (filterStudy == null) {
            data = activityController.getPaginatedActivityDetail(first, pageSize);
            //TODO: add support for sorting, filtering
        }else{
            data = activityController.getPaginatedActivityDetailForStudy(first,pageSize,filterStudy);
        }
        if (first == 0) {
            this.data = data;
        }
        return data;
    }

    @Override
    public void setWrappedData(Object list) {
        super.setWrappedData(list);
        if (data == null) {
            data = new ArrayList<>();
        }
        data.addAll((List<ActivityDetail>) list);
    }

    @Override
    public Object getWrappedData() {
        return new Vector<>(data); //Converting to Vector probably needed because it's seemingly cast to a Vector in PrimeFaces
    }

}
