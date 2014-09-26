package se.kth.kthfsdashboard.user;

import java.io.*;
import java.security.*;

/**
 * Custom class to get the gravatar image from an email. Implemented because of
 * issues with the jgravatar library.
 *
 * @author Stig
 */
public class Gravatar {

    private static final String BASIC_URL = "http://www.gravatar.com/avatar/";

    public static String hex(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i]
                    & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    public static String md5Hex(String message) {
        try {
            MessageDigest md
                    = MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
        }
        return null;
    }

    public static String getUrl(String email) {
        String hash = md5Hex(email);
        return BASIC_URL + hash;
    }

    public static String getUrl(String email, int size) {
        String url = getUrl(email);
        return url + "?s=" + size;
    }

}
