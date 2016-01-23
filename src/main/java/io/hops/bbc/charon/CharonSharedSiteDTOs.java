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
public class CharonSharedSiteDTOs implements Serializable {

    private List<CharonSharedSiteDTO> sharedRepos;

    public CharonSharedSiteDTOs() {
    }

    public CharonSharedSiteDTOs(List<CharonSharedSiteDTO> sharedRepos) {
        this.sharedRepos = sharedRepos;
    }

    public List<CharonSharedSiteDTO> getSharedRepos() {
        return sharedRepos;
    }

    public void setSharedRepos(List<CharonSharedSiteDTO> registeredSites) {
        this.sharedRepos = registeredSites;
    }

}
