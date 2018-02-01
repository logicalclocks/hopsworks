/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CsrDTO implements Serializable {

  private String caPubCert;
  private String intermediateCaPubCert;
  private String pubAgentCert;
  private String hadoopHome;

  public CsrDTO() {
  }

  public CsrDTO(String caPubCert, String pubAgentCert, String hadoopHome) {
    this.caPubCert = caPubCert;
    this.pubAgentCert = pubAgentCert;
    this.hadoopHome = hadoopHome;
  }

  public CsrDTO(String caPubCert, String intermediateCaPubCert, String pubAgentCert, String hadoopHome) {
    this.caPubCert = caPubCert;
    this.intermediateCaPubCert = intermediateCaPubCert;
    this.pubAgentCert = pubAgentCert;
    this.hadoopHome = hadoopHome;
  }

  public String getHadoopHome() {
    return hadoopHome;
  }

  public void setHadoopHome(String hadoopHome) {
    this.hadoopHome = hadoopHome;
  }

  public String getCaPubCert() {
    return caPubCert;
  }

  public String getIntermediateCaPubCert() {
    return intermediateCaPubCert;
  }

  public void setIntermediateCaPubCert(String intermediateCaPubCert) {
    this.intermediateCaPubCert = intermediateCaPubCert;
  }

  public String getPubAgentCert() {
    return pubAgentCert;
  }

  public void setCaPubCert(String caPubCert) {
    this.caPubCert = caPubCert;
  }

  public void setPubAgentCert(String pubAgentCert) {
    this.pubAgentCert = pubAgentCert;
  }

}
