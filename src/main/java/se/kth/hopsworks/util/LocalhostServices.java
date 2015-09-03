package se.kth.hopsworks.util;

import se.kth.bbc.lims.Constants;

import java.io.IOException;

public class LocalhostServices {

    public static void createUserAccount(String username, String projectName) throws IOException {

        String user = getProjectUsername(username, projectName);

        // --gecos "" - enables non-interactive
        // /usr/sbin/adduser --home /srv/users/johnny --shell /bin/bash --ingroup hadoop --gecos "" --disabled-password johnny
        // /usr/sbin/deluser johnny

        // mkdir .ssh
        // touch .ssh/authorized_keys

        // initialize .bashrc file

    }
    public static void deleteUserAccount(String username, String projectName) throws IOException {

    }

    public static String getProjectUsername(String username, String projectName) {
        return username + Constants.HOPS_USERNAME_SEPARATOR + projectName;
    }
}
