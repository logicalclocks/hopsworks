/*
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
 *
 */

package io.hops.hopsworks.ca.apiV2.certificates;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@ApiModel(value = "Represents a certificate signing request")
public class CSRView {
  private String csr;
  private String signedCert;
  private String intermediateCaCert;
  private String rootCaCert;

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
}
