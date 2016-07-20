package se.kth.hopsworks.util;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import javax.naming.NamingException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class DatabasePersTest {
     
  @BeforeClass
  public static void setUpClass() throws NamingException{
   System.out.println("Setup Class"); 
  }
  
  @AfterClass
  public static void tearDownClass() {
      System.out.println("TearDown Class");
  }
  
  @Before
    public void setUp() {
    System.out.println("Setup");
    }

  @After
    public void tearDown() {
    System.out.println("TearDown");
    }
  
    
  @Test
  public void myTest(){
      System.out.println("Test Starts");
      String jarPath = "hdfs://10.0.2.15:8020/Projects/myFirstProject/Resources/spark-examples-1.5.2-hadoop2.4.0.jar";
      String[] parts = jarPath.split("/");
      Assert.assertEquals("First part should be hdfs:", parts[0], "hdfs:");
      Assert.assertEquals("Second part should be /", parts[1], "");
      Assert.assertEquals("Third part should be Projects:", parts[2], "10.0.2.15:8020");
      String realPath = "/Projects/myFirstProject/Resources/spark-examples-1.5.2-hadoop2.4.0.jar";
      String test = jarPath.replace("hdfs://" + parts[2], "");
      Assert.assertEquals("Should be equal", test, realPath);
  }  
}
