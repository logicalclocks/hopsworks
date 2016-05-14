/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author misdess
 */
@XmlRootElement
public class HdfsUserDTO {
    private String userName;

    public HdfsUserDTO() {
    }

    public HdfsUserDTO(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    
}
