/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jdowling
 */
public class UserAccountsTest {
  
  public UserAccountsTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

    // TODO add test methods here.
  // The methods must be annotated with annotation @Test. For example:
  //
   @Test
   public void createAndDestroyAccountsTest() {
   
    try {
      List<String> keys = new ArrayList<>();
      keys.add("my key public ssh key....");
      LocalhostServices.createUserAccount("james", "project", keys);
      
      LocalhostServices.deleteUserAccount("james", "project");
    } catch (IOException ex) {
      Logger.getLogger(UserAccountsTest.class.getName()).log(Level.SEVERE, null, ex);
      assertFalse(true);
    }
   
   }
}
