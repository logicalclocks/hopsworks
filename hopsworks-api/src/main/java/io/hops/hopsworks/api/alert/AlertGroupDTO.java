package io.hops.hopsworks.api.alert;

import io.hops.hopsworks.alerting.api.alert.dto.AlertGroup;
import io.hops.hopsworks.alerting.api.alert.dto.ReceiverName;
import io.hops.hopsworks.common.api.RestDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlertGroupDTO extends RestDTO<AlertGroupDTO> {
  private Map<String, String> labels;
  private ReceiverName receiver;
  private List<AlertDTO> alertDTOs;

  public AlertGroupDTO() {
  }

  public AlertGroupDTO(AlertGroup alertGroup) {
    this.labels = alertGroup.getLabels();
    this.receiver = alertGroup.getReceiver();
    this.alertDTOs = alertGroup.getAlerts().stream().map(AlertDTO::new).collect(Collectors.toList());
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public ReceiverName getReceiver() {
    return receiver;
  }

  public void setReceiver(ReceiverName receiver) {
    this.receiver = receiver;
  }

  public List<AlertDTO> getAlertDTOs() {
    return alertDTOs;
  }

  public void setAlertDTOs(List<AlertDTO> alertDTOs) {
    this.alertDTOs = alertDTOs;
  }

  @Override
  public String toString() {
    return "AlertGroupDTO{" +
        "labels=" + labels +
        ", receiver=" + receiver +
        ", alertDTOs=" + alertDTOs +
        '}';
  }
}
