package io.hops.hopsworks.common.api;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CollectionsBuilder<T> {
  
  protected abstract List<T> getItems(Project project);
  
  protected abstract List<T> filterTopics(List<T> list, AbstractFacade.FilterBy filterBy);
  
  protected abstract List<T> sortTopics(List<T> list, Set<? extends AbstractFacade.SortBy> sortBySet);
  
  public List<T> buildItems(Project project, ResourceRequest resourceRequest) {
    List<T> list = getItems(project);
  
    for (AbstractFacade.FilterBy tFilter : resourceRequest.getFilter()) {
      list = filterTopics(list, tFilter);
    }
  
    list = sortTopics(list, resourceRequest.getSort());
  
    list = paginate(list, resourceRequest.getLimit(), resourceRequest.getOffset());
  
    return list;
  }
  
  List<T> paginate(List<T> list, Integer limit, Integer offset) {
    Stream<T> stream = list.stream();
    if (offset != null) {
      stream = stream.skip(offset);
    }
    if (limit != null) {
      stream = stream.limit(limit);
    }
    return stream.collect(Collectors.toList());
  }
}
