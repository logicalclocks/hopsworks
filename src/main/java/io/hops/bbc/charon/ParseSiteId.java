/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.bbc.charon;

/**
 *
 * @author jdowling
 */
public class ParseSiteId {

    private static String keySearch(String siteId, String key) {
        String[] lines = siteId.split("\\r?\\n");
        if (lines == null || lines.length < 4) {
            return "Bad siteID";
        }
        String res = "";
        for (String l : lines) {
            String[] kv = l.split("=");
            if (kv == null || kv.length < 2) {
                continue;
            }
            String keyFound = kv[0].trim();
            if (keyFound.compareToIgnoreCase(key) == 0) {
                return kv[1];
            }
        }
        return "Not found: kv pair";
    }

    public static int id(String siteId) {
        String id = keySearch(siteId, "id");
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    public static String name(String siteId) {
        return keySearch(siteId, "name");
    }

    public static String email(String siteId) {
        return keySearch(siteId, "email");
    }

    public static String addr(String siteId) {
        return keySearch(siteId, "addr");
    }

}
