package io.hops.hopsworks.common.dao.device;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AckRecordDTO {

  private boolean ack;

  public AckRecordDTO() {
  }

  public AckRecordDTO(boolean ack) {
    this.ack = ack;
  }

  public boolean isAck() {
    return ack;
  }

  public void setAck(boolean ack) {
    this.ack = ack;
  }
}
