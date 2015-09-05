package se.kth.bbc.project.fb;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

/**
 * Convert between MySQL bit(8) and Java byte.
 * <p>
 * @author stig
 */
public class ByteConverter implements Converter {

  /**
   * Not tested 
   * @param o
   * @param sn
   * @return 
   */
  @Override
  public Object convertObjectValueToDataValue(Object o, Session sn) {
    byte[] b = new byte[1];
    b[0] = (byte) o;
    return b;
  }

  @Override
  public Object convertDataValueToObjectValue(Object o, Session sn) {
    byte[] b = (byte[])o;
    return b[0];
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
