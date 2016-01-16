/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.bbc.charon;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CharonRegisteredSiteDTOs implements Serializable {

    private List<CharonRegisteredSiteDTO> registeredSites;

    public CharonRegisteredSiteDTOs() {
    }

    public CharonRegisteredSiteDTOs(List<CharonRegisteredSiteDTO> registeredSites) {
        this.registeredSites = registeredSites;
    }

    public List<CharonRegisteredSiteDTO> getRegisteredSites() {
        return registeredSites;
    }

    public void setRegisteredSites(List<CharonRegisteredSiteDTO> registeredSites) {
        this.registeredSites = registeredSites;
    }

}
