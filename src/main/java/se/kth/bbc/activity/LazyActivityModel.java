package se.kth.bbc.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

/**
 * Model for lazily loading activity data into index page. Some explanations:
 * PrimeFaces sucks. The code for LazyDataModel as it is right now does not
 * allow for usage of commandLink or commandButton in a p:datascroller.
 *
 * Hence, here most of the methods of LazyDataModel are overridden. Basically,
 * all the data that has been loaded so far is saved and kept track of. Then
 * when the RowIndex is set, instead of taking it modulo the page size, it is
 * taken literally as an index to the data list. This ensures that the same data
 * is always returned, as is necessary when using an iterating JSF component.
 *
 * Because setWrappedData is possibly called several times with the same data,
 * new data is stored in the load method.
 *
 * @author stig
 */
public class LazyActivityModel extends LazyDataModel<ActivityDetail> implements Serializable {

    private transient final ActivityController activityController;
    private List<ActivityDetail> data;
    private String filterStudy;
    private int rowIndex;

    public LazyActivityModel(ActivityController ac) {
        this(ac, null);
    }

    public LazyActivityModel(ActivityController ac, String filterStudy) {
        super();
        this.activityController = ac;
        this.filterStudy = filterStudy;
        data = new ArrayList<>();
    }

    @Override
    public List<ActivityDetail> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        List<ActivityDetail> retData;
        if (filterStudy == null) {
            retData = activityController.getPaginatedActivityDetail(first, pageSize);
            //TODO: add support for sorting, filtering
        } else {
            retData = activityController.getPaginatedActivityDetailForStudy(first, pageSize, filterStudy);
        }
        if (first == 0) {
            data = new ArrayList<>(retData);
            return retData;
        } else if (first >= data.size()) {
            data.addAll(retData);
            return retData;
        } else {
            return data.subList(first, Math.min(first + pageSize, data.size()));
        }
    }

    @Override
    public void setRowIndex(int index) {
        if (index >= data.size()) {
            index = -1;
        }
        this.rowIndex = index;
    }

    @Override
    public ActivityDetail getRowData() {
        return data.get(rowIndex);
    }

    /**
     * Overriden because default implementation checks super.data against null.
     *
     * @return
     */
    @Override
    public boolean isRowAvailable() {
        if (data == null) {
            return false;
        }
        return rowIndex >= 0 && rowIndex < data.size();
    }

}
