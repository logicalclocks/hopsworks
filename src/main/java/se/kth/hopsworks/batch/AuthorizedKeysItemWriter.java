package se.kth.hopsworks.batch;

import java.util.List;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.inject.Named;

@Named
public class AuthorizedKeysItemWriter extends AbstractItemWriter {

  @Override
  public void writeItems(List<Object> items) throws Exception {
  }

}
