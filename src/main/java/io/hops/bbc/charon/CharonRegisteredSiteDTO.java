/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.bbc.charon;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CharonRegisteredSiteDTO implements Serializable {

    private String name;
    private int siteId;
    private String email;
    private String addr;

    public CharonRegisteredSiteDTO(CharonRegisteredSites site) {
        this.siteId = site.getCharonRegisteredSitesPK().getSiteId();
        this.name = site.getName();
        this.email = site.getEmail();
        this.addr = site.getAddr();
    }
    public CharonRegisteredSiteDTO(String name, int siteID, String email, String addr) {
        this.name = name;
        this.siteId = siteID;
        this.email = email;
        this.addr = addr;
    }

    public CharonRegisteredSiteDTO() {
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteID) {
        this.siteId = siteID;
    }

    public String getAddr() {
        return addr;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Name: " + name + " ; id: " + siteId;
    }

}
