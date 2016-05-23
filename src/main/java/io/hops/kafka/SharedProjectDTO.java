/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author misdess
 */
@XmlRootElement
public class SharedProjectDTO implements Serializable{
    
    private String name;
    private Integer id;
    

    public SharedProjectDTO() {
    }

    public SharedProjectDTO(String name) {
        this.name = name;
    }

    public SharedProjectDTO(String name, Integer id) {
        this.name = name;
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
}
