package se.kth.hopsworks.meta.entity;

/**
 *
 * @author Vangelis
 */
public interface EntityIntf {

  /**
   * Return the id of this entity
   * <p/>
   * @return the integer id
   */
  public abstract Integer getId();

  /**
   * Sets the id of this entity
   * <p/>
   * @param id the new id of this entity
   */
  public abstract void setId(Integer id);

  /**
   * Copies another entity into this one. Convenient when updating entities
   * through JPA
   * <p/>
   * @param entity the entity to be copied to the current one
   */
  public abstract void copy(EntityIntf entity);

}
