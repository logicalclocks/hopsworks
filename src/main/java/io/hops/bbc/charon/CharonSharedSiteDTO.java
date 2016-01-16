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

    private int granteeId;
    private String token;
    private String path;
    private String permissions;

    public CharonSharedSiteDTO() {
    }

    public CharonSharedSiteDTO(CharonRepoShared crs) {
        this.granteeId = crs.getCharonRepoSharedPK().getSiteId();
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

    public int getGranteeId() {
        return granteeId;
    }

    public void setGranteeId(int granteeID) {
        this.granteeId = granteeID;
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
        return "siteId: " + granteeId + " ; path: " + path + "; token: " + token;
    }

    
}
