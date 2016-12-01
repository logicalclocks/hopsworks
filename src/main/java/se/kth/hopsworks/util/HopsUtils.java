package se.kth.hopsworks.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
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
  public static <E extends Enum<E>> boolean isInEnum(String value,
          Class<E> enumClass) {
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
    return calculatePartitionId(ROOT_INODE_ID, PROJECTS_DIR_NAME,
            PROJECTS_DIR_DEPTH);
  }

  public static int dataSetPartitionId(Inode parent, String name) {
    return calculatePartitionId(parent.getId(), name, 3);
  }

  public static int calculatePartitionId(int parentId, String name, int depth) {
    if (isTreeLevelRandomPartitioned(depth)) {
      return partitionIdHashFunction(parentId, name, depth);
    } else {
      return parentId;
    }
  }

  private static int partitionIdHashFunction(int parentId, String name,
          int depth) {
    if (depth == ROOT_DIR_DEPTH) {
      return ROOT_DIR_PARTITION_KEY;
    } else {
      return (name + parentId).hashCode();
    }
  }

  private static boolean isTreeLevelRandomPartitioned(int depth) {
    return depth <= RANDOM_PARTITIONING_MAX_LEVEL;
  }

  /**
   * Retrieves the global hadoop classpath.
   *
   * @param params
   * @return
   */
  public static String getHadoopClasspathGlob(String... params) {
    ProcessBuilder pb = new ProcessBuilder(params);
    try {
      Process process = pb.start();
      int errCode = process.waitFor();
      if (errCode != 0) {
        return "";
      }
      StringBuilder sb = new StringBuilder();
      try (BufferedReader br
              = new BufferedReader(new InputStreamReader(process.
                      getInputStream()))) {
        String line;
        while ((line = br.readLine()) != null) {
          sb.append(line);
        }
      }
      //Now we must remove the yarn shuffle library as it creates issues for 
      //Zeppelin Spark Interpreter
      StringBuilder classpath = new StringBuilder();
      
      for(String path : sb.toString().split(File.pathSeparator)){
        if(!path.contains("yarn")){
          classpath.append(path).append(File.pathSeparator);
        }
      }
      if(classpath.length()>0){
        return classpath.toString().substring(0,classpath.length()-1);
      }

    } catch (IOException | InterruptedException ex) {
      Logger.getLogger(HopsUtils.class.getName()).log(Level.SEVERE, null, ex);
    }
    return "";
  }

}
