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
