package se.kth.meta.entity.listener;

import se.kth.meta.entity.Tables;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

/**
 *
 * @author Vangelis
 */
public class EntityListener {

  //Listener in case we need to handle the entities before or after db
  //operations take place. Should be removed probably
  @PrePersist
  void onPrePersist(Object o) {
  }

  @PostPersist
  void onPostPersist(Object o) {
    Tables t = (Tables) o;
  }

  @PostLoad
  void onPostLoad(Object o) {
  }

  @PreUpdate
  void onPreUpdate(Object o) {
  }

  @PostUpdate
  void onPostUpdate(Object o) {
  }

  @PreRemove
  void onPreRemove(Object o) {
  }

  @PostRemove
  void onPostRemove(Object o) {
  }
}
