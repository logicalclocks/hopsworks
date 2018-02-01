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

package io.hops.hopsworks.dela.old_dto;

import java.util.List;

public class ExtendedDetails {

  private List<HdfsDetails> hdfsDetails;
  private List<KafkaDetails> kafkaDetails;

  public ExtendedDetails() {
  }

  public ExtendedDetails(List<HdfsDetails> hdfsDetails, List<KafkaDetails> kafkaDetails) {
    this.hdfsDetails = hdfsDetails;
    this.kafkaDetails = kafkaDetails;
  }

  public List<HdfsDetails> getHdfsDetails() {
    return hdfsDetails;
  }

  public void setHdfsDetails(List<HdfsDetails> hdfsDetails) {
    this.hdfsDetails = hdfsDetails;
  }

  public List<KafkaDetails> getKafkaDetails() {
    return kafkaDetails;
  }

  public void setKafkaDetails(List<KafkaDetails> kafkaDetails) {
    this.kafkaDetails = kafkaDetails;
  }
}
