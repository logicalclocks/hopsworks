/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class TopicDTO {

  private String topic;
  // TODO - put in all the topic details here

  public TopicDTO() {
  }
  public TopicDTO(String topic) {
    this.topic = topic;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

}
