/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.bbc.charon;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CharonSharedSiteDTO implements Serializable {

    private int granteeID;
    private String token;
    private String path;
    private String permissions;

    public CharonSharedSiteDTO() {
    }

    public CharonSharedSiteDTO(CharonRepoShared crs) {
        this.granteeID = crs.getCharonRepoSharedPK().getSiteId();
        this.path = crs.getCharonRepoSharedPK().getPath();
        this.permissions = crs.getPermissions();
        this.token = crs.getToken();
    }
    
    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public String getPath() {
        return path;
    }

    public int getGranteeID() {
        return granteeID;
    }

    public void setGranteeID(int granteeID) {
        this.granteeID = granteeID;
    }

    
    public String getToken() {
        return token;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "siteId: " + granteeID + " ; path: " + path + "; token: " + token;
    }

    
}
