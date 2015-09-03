package se.kth.hopsworks.util;

import se.kth.bbc.lims.Constants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocalhostServices {

  public static String createUserAccount(String username, String projectName, List<String> sshKeys) throws IOException {

    String user = getProjectUsername(username, projectName);
    // /usr/sbin/adduser --home /srv/users/johnny --shell /bin/bash --ingroup hadoop --gecos "" --disabled-password johnny

    String home = "/srv/users/" + user;

    if (new File(home).exists()) {
      throw new IOException("Home directory already exists: " + home);
    }
    List<String> commands = new ArrayList<String>();
    commands.add("/bin/bash");
    commands.add("-c");
    commands.add("sudo /usr/sbin/adduser --home " + home + " --shell /bin/bash --ingroup hadoop --gecos \"\" "
        + "--disabled-password " + username);

    SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands);
    String stdout = "", stderr = "";
    try {
      int result = commandExecutor.executeCommand();
      // get the stdout and stderr from the command that was run
      stdout = commandExecutor.getStandardOutputFromCommand();
      stderr = commandExecutor.getStandardErrorFromCommand();
      if (result != 0) {
        throw new IOException("Could not create user: " + home + " - " + stderr);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new IOException("Interrupted. Could not create user: " + home + " - " + stderr);
    }

    File sshDir = new File(home + "/.ssh");
    sshDir.mkdir();

    File authorizedKeys = new File(home + "/.ssh/authorized_keys");
    authorizedKeys.createNewFile();

    Path path = authorizedKeys.toPath();
    // No need for a BufferedWriter, it's only a small file.
    Files.write(path, sshKeys, Constants.ENCODING);

    // Change the permissions of the .ssh directory and authorized_keys files.
    if (System.getProperty("os.name").toLowerCase().indexOf("win") == -1) {
//        chmod 700 ~/.ssh
      Set<PosixFilePermission> sshDirPerms = new HashSet<>();
      sshDirPerms.add(PosixFilePermission.OWNER_READ);
      sshDirPerms.add(PosixFilePermission.OWNER_WRITE);
      sshDirPerms.add(PosixFilePermission.OWNER_EXECUTE);
      sshDirPerms.remove(PosixFilePermission.GROUP_READ);
      sshDirPerms.remove(PosixFilePermission.GROUP_WRITE);
      sshDirPerms.remove(PosixFilePermission.GROUP_EXECUTE);
      sshDirPerms.remove(PosixFilePermission.OTHERS_READ);
      sshDirPerms.remove(PosixFilePermission.OTHERS_WRITE);
      sshDirPerms.remove(PosixFilePermission.OTHERS_EXECUTE);
      Files.setPosixFilePermissions(sshDir.toPath(), sshDirPerms);

//        chmod 600 ~/.ssh/authorized_keys
      Set<PosixFilePermission> authorizedKeysPerms = new HashSet<>();
      authorizedKeysPerms.add(PosixFilePermission.OWNER_READ);
      authorizedKeysPerms.add(PosixFilePermission.OWNER_WRITE);
      authorizedKeysPerms.remove(PosixFilePermission.OWNER_EXECUTE);
      authorizedKeysPerms.remove(PosixFilePermission.GROUP_READ);
      authorizedKeysPerms.remove(PosixFilePermission.GROUP_EXECUTE);
      authorizedKeysPerms.remove(PosixFilePermission.GROUP_WRITE);
      authorizedKeysPerms.remove(PosixFilePermission.OTHERS_READ);
      authorizedKeysPerms.remove(PosixFilePermission.OTHERS_EXECUTE);
      authorizedKeysPerms.remove(PosixFilePermission.OTHERS_WRITE);
      Files.setPosixFilePermissions(authorizedKeys.toPath(), authorizedKeysPerms);
    }
    return stdout;
  }

  public static String deleteUserAccount(String username, String projectName) throws IOException {
    // /usr/sbin/deluser johnny

    String user = getProjectUsername(username, projectName);
    String home = "/srv/users/" + user;

    if (new File(home).exists() == false) {
      throw new IOException("Home directory does not exist: " + home);
    }
    List<String> commands = new ArrayList<String>();
    commands.add("/bin/bash");
    commands.add("-c");
    commands.add("sudo /usr/sbin/deluser " + user);

    SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands);
    String stdout = "", stderr = "";
    try {
      int result = commandExecutor.executeCommand();
      // get the stdout and stderr from the command that was run
      stdout = commandExecutor.getStandardOutputFromCommand();
      stderr = commandExecutor.getStandardErrorFromCommand();
      if (result != 0) {
        throw new IOException("Could not delete user " + home + " - " + stderr);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new IOException("Interrupted. Could not delete user: " + home + " - " + stderr);
    }
    return stdout;
  }

  public static String getProjectUsername(String username, String projectName) {

    if (username.contains("@")) {
      username = username.substring(0, username.lastIndexOf("@") - 1);
    }

    return username + Constants.HOPS_USERNAME_SEPARATOR + projectName;
  }
}
