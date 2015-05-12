
package se.kth.meta.entity;

/**
 *
 * @author Vangelis
 */
public interface EntityIntf {

    
    /**
     * Return the id of this entity
     * 
     * @return the integer id
     */
    public abstract Integer getId();
    
    /**
     * Sets the id of this entity
     * 
     * @param id the new id of this entity
     */
    public abstract void setId(Integer id);
    
    /**
     * Copies another entity into this one. Convenient when updating entities
     * through JPA
     * 
     * @param entity the entity to be copied to the current one
     */
    public abstract void copy(EntityIntf entity);
    
}
