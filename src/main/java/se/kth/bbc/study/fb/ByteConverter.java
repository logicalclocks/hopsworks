package se.kth.bbc.study.fb;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

/**
 * Convert between MySQL bit(8) and Java byte.
 * <p>
 * @author stig
 */
public class ByteConverter implements Converter {

  @Override
  public Object convertObjectValueToDataValue(Object o, Session sn) {
    return Byte.MIN_VALUE;
  }

  @Override
  public Object convertDataValueToObjectValue(Object o, Session sn) {
    return Byte.MIN_VALUE;
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public void initialize(DatabaseMapping dm, Session sn) {
    //Do nothing  
  }

}
