/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.batch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.batch.api.chunk.ItemProcessor;
import javax.ejb.EJB;
import javax.inject.Named;
import se.kth.bbc.lims.Constants;
import se.kth.hopsworks.user.model.SshKeys;
import se.kth.hopsworks.users.SshkeysFacade;
import se.kth.hopsworks.util.LocalhostServices;
import static se.kth.hopsworks.util.LocalhostServices.getUsernameInProject;

@Named
public class AuthorizedKeysItemProcessor implements ItemProcessor {

  @EJB
  private SshkeysFacade sshKeysBean;

  @Override
  public String processItem(Object item) throws Exception {
    Map<Integer, List<String>> usernames = (Map<Integer, List<String>>) item;

    for (Integer id : usernames.keySet()) {
      List<SshKeys> keys = sshKeysBean.findAllById(id);
      // now add all these ssh public keys to the .ssh/authorized_keys files on all project-user accounts
      List<String> sshKeys = new ArrayList<>();
      for (SshKeys k : keys) {
        sshKeys.add(k.getPublicKey());
      }

      // For each project-user, add a
      for (String user : usernames.get(id)) {
        String home = "/srv/users/" + user;
        File authorizedKeys = new File(home + "/.ssh/authorized_keys");
        authorizedKeys.delete();
        authorizedKeys.createNewFile();
        Path path = authorizedKeys.toPath();
        if (!sshKeys.isEmpty()) {
          Files.write(path, sshKeys, Constants.ENCODING);
        }
      }
    }
    return "";
  }

}
