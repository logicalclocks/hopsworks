package se.kth.hopsworks.util;

import se.kth.bbc.project.fb.Inode;

/**
 * Utility methods.
 * <p>
 */
public class HopsUtils {

  public static final int ROOT_DIR_PARTITION_KEY = 0;
  public static final short ROOT_DIR_DEPTH = 0;
  public static int RANDOM_PARTITIONING_MAX_LEVEL = 1;
  public static int ROOT_INODE_ID = 1;
  public static int PROJECTS_DIR_DEPTH = 1;
  public static String PROJECTS_DIR_NAME = "Projects";

  /**
   *
   * @param <E>
   * @param value
   * @param enumClass
   * @return
   */
  public static <E extends Enum<E>> boolean isInEnum(String value, Class<E> enumClass) {
    for (E e : enumClass.getEnumConstants()) {
      if (e.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
  
  public static int fileOrDirPartitionId(int parentId, String name) {
    return parentId;
  }  

  public static int projectPartitionId(String name) {
    return calculatePartitionId(ROOT_INODE_ID, PROJECTS_DIR_NAME, PROJECTS_DIR_DEPTH);
  }
  
  public static int dataSetPartitionId(Inode parent, String name) {
    return parent.getId();
  }  
  
  public static int calculatePartitionId(int parentId, String name, int depth) {
    if (isTreeLevelRandomPartitioned( depth)) {
      return partitionIdHashFunction(parentId, name,depth);
    } else {
      return parentId;
    }
  }

  private static int partitionIdHashFunction(int parentId, String name, int depth) {
    if (depth == ROOT_DIR_DEPTH) {
      return ROOT_DIR_PARTITION_KEY;
    } else {
      return (name + parentId).hashCode();
    }
  }

  private static boolean isTreeLevelRandomPartitioned(int depth) {
    return depth <= RANDOM_PARTITIONING_MAX_LEVEL;
  }
}
