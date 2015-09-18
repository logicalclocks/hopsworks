package se.kth.hopsworks.filters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Annotations that can be used to restrict users from accessing project methods
 * based on the role they have for that project.
 * For this annotation to work the method annotated should be a web service with
 * a path project/{id}/*.
 * if no role is specified the default will be OWNER only access
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AllowedRoles {

  /**
   * Allowed for everyone. This does not mean both roles it means literally
   * everyone
   */
  public static final String ALL = "ALL";
  /**
   * Allowed only to the owner
   */
  public static final String DATA_OWNER = "Data owner";
  /**
   * Allowed to contributers or members of the project. There is no hierarchy if
   * only this annotation is used only members will be granted access. So to
   * allow
   * owners and members use both.
   */
  public static final String DATA_SCIENTIST = "Data scientist";

  /**
   * Used to annotate methods that work with project resources
   * <p>
   * @return allowed roles
   */
  public String[] roles() default {AllowedRoles.DATA_OWNER};
}
