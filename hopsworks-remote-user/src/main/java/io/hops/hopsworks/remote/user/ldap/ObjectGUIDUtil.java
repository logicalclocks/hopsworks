/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.ldap;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ObjectGUIDUtil {

  public ObjectGUIDUtil() {
  }

  private String[] convertToOctetString(String dashedString) {
    String[] tokens = dashedString.split("-");
    String[] objectGUID = new String[16];
    objectGUID[0] = tokens[0].substring(6, 8);
    objectGUID[1] = tokens[0].substring(4, 6);
    objectGUID[2] = tokens[0].substring(2, 4);
    objectGUID[3] = tokens[0].substring(0, 2);

    objectGUID[4] = tokens[1].substring(2, 4);
    objectGUID[5] = tokens[1].substring(0, 2);

    objectGUID[6] = tokens[2].substring(2, 4);
    objectGUID[7] = tokens[2].substring(0, 2);

    objectGUID[8] = tokens[3].substring(0, 2);
    objectGUID[9] = tokens[3].substring(2, 4);

    objectGUID[10] = tokens[4].substring(0, 2);
    objectGUID[11] = tokens[4].substring(2, 4);
    objectGUID[12] = tokens[4].substring(4, 6);
    objectGUID[13] = tokens[4].substring(6, 8);
    objectGUID[14] = tokens[4].substring(8, 10);
    objectGUID[15] = tokens[4].substring(10, 12);

    return objectGUID;
  }

  /**
   * From:
   * http://www.developerscrappad.com/1109/windows/active-directory/java-ldap-jndi-2-ways-of-decoding-and-using-the-
   * objectguid-from-windows-active-directory/
   *
   * @param objectGUID
   * @return
   */
  public String convertToDashedString(byte[] objectGUID) {
    StringBuilder displayStr = new StringBuilder();

    displayStr.append(prefixZeros((int) objectGUID[3] & 0xFF));
    displayStr.append(prefixZeros((int) objectGUID[2] & 0xFF));
    displayStr.append(prefixZeros((int) objectGUID[1] & 0xFF));
    displayStr.append(prefixZeros((int) objectGUID[0] & 0xFF));
    displayStr.append("-");
    displayStr.append(prefixZeros((int) objectGUID[5] & 0xFF));
    displayStr.append(prefixZeros((int) objectGUID[4] & 0xFF));
    displayStr.append("-");
    displayStr.append(prefixZeros((int) objectGUID[7] & 0xFF));
    displayStr.append(prefixZeros((int) objectGUID[6] & 0xFF));
    displayStr.append("-");
    displayStr.append(prefixZeros((int) objectGUID[8] & 0xFF));
    displayStr.append(prefixZeros((int) objectGUID[9] & 0xFF));
    displayStr.append("-");
    displayStr.append(prefixZeros((int) objectGUID[10] & 0xFF));
    displayStr.append(prefixZeros((int) objectGUID[11] & 0xFF));
    displayStr.append(prefixZeros((int) objectGUID[12] & 0xFF));
    displayStr.append(prefixZeros((int) objectGUID[13] & 0xFF));
    displayStr.append(prefixZeros((int) objectGUID[14] & 0xFF));
    displayStr.append(prefixZeros((int) objectGUID[15] & 0xFF));

    return displayStr.toString();
  }

  public String convertToByteString(String stringGUID) {
    String[] objectGUID = convertToOctetString(stringGUID);
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < objectGUID.length; i++) {
      String transformed = objectGUID[i];
      result.append("\\");
      result.append(transformed);
    }
    return result.toString();
  }

  private String prefixZeros(int value) {
    if (value <= 0xF) {
      StringBuilder sb = new StringBuilder("0");
      sb.append(Integer.toHexString(value));
      return sb.toString();
    } else {
      return Integer.toHexString(value);
    }
  }

}
