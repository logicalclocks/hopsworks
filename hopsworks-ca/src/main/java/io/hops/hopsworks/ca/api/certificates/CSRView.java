/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.ca.api.certificates;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@ApiModel(value = "Represents a certificate signing request")
public class CSRView {
  private String privateKey;
  private String csr;
  private String signedCert;
  private String intermediateCaCert;
  private String rootCaCert;
  private String keyStore;
  private String trustStore;
  private String password;

  public CSRView() {

  }

  public CSRView(String csr, String signedCert, String rootCaCert, String intermediateCaCert) {
    this.csr = csr;
    this.signedCert = signedCert;
    this.intermediateCaCert = intermediateCaCert;
    this.rootCaCert = rootCaCert;
  }

  public CSRView(String signedCert, String rootCaCert, String intermediateCaCert) {
    this.signedCert = signedCert;
    this.intermediateCaCert = intermediateCaCert;
    this.rootCaCert = rootCaCert;
  }

  public CSRView(String rootCaCert, String intermediateCaCert) {
    this.intermediateCaCert = intermediateCaCert;
    this.rootCaCert = rootCaCert;
  }

  @ApiModelProperty(value = "PKCS8 encoded private key used to create Java keystore")
  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }

  @ApiModelProperty(value = "String containing the certificate signing request", required = true)
  public String getCsr() {
    return csr;
  }

  public void setCsr(String csr) {
    this.csr = csr;
  }

  @ApiModelProperty(value = "String containing the certificate signed by the Intermediate CA", readOnly = true)
  public String getSignedCert() {
    return signedCert;
  }

  public void setSignedCert(String signedCert) {
    this.signedCert = signedCert;
  }

  @ApiModelProperty(value = "String containing the certificate of the Intermediate CA", readOnly = true)
  public String getIntermediateCaCert() {
    return intermediateCaCert;
  }

  public void setIntermediateCaCert(String intermediateCaCert) {
    this.intermediateCaCert = intermediateCaCert;
  }

  @ApiModelProperty(value = "String containing the certificate of the Root CA", readOnly = true)
  public String getRootCaCert() {
    return rootCaCert;
  }

  public void setRootCaCert(String rootCaCert) {
    this.rootCaCert = rootCaCert;
  }

  @ApiModelProperty(value = "Base64 encoded keystore containing signed certificate and intermediate CA locked by " +
      "password")
  public String getKeyStore() {
    return keyStore;
  }

  public void setKeyStore(String keyStore) {
    this.keyStore = keyStore;
  }

  @ApiModelProperty(value = "Base64 encoded trustore containing Hopsworks root CA certificate locked by password")
  public String getTrustStore() {
    return trustStore;
  }

  public void setTrustStore(String trustStore) {
    this.trustStore = trustStore;
  }

  @ApiModelProperty(value = "Randomly generated password to protect keystore and truststore")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
