/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SchemaDTO implements Serializable {


    private String name;
    private String contents;
    private int version;

    public SchemaDTO() {
    }

    public SchemaDTO(String name, String contents, int version) {
        this.name = name;
        this.contents = contents;
        this.version = version;
    }

    public String getContents() {
        return contents;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(int version) {
        this.version = version;
    }
    
}
