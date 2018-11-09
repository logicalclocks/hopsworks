/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.security;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CSR {
  private String csr;
  private String signedCert;
  private String intermediateCaCert;
  private String rootCaCert;

  public CSR() { }

  public CSR(String csr) {
    this.csr = csr;
  }

  public CSR(String csr, String signedCert, String rootCaCert, String intermediateCaCert) {
    this.csr = csr;
    this.signedCert = signedCert;
    this.intermediateCaCert = intermediateCaCert;
    this.rootCaCert = rootCaCert;
  }

  public CSR(String signedCert, String rootCaCert, String intermediateCaCert) {
    this.signedCert = signedCert;
    this.intermediateCaCert = intermediateCaCert;
    this.rootCaCert = rootCaCert;
  }

  public CSR(String rootCaCert, String intermediateCaCert) {
    this.intermediateCaCert = intermediateCaCert;
    this.rootCaCert = rootCaCert;
  }

  public String getCsr() {
    return csr;
  }

  public void setCsr(String csr) {
    this.csr = csr;
  }

  public String getSignedCert() {
    return signedCert;
  }

  public void setSignedCert(String signedCert) {
    this.signedCert = signedCert;
  }

  public String getIntermediateCaCert() {
    return intermediateCaCert;
  }

  public void setIntermediateCaCert(String intermediateCaCert) {
    this.intermediateCaCert = intermediateCaCert;
  }

  public String getRootCaCert() {
    return rootCaCert;
  }

  public void setRootCaCert(String rootCaCert) {
    this.rootCaCert = rootCaCert;
  }
}
