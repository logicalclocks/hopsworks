/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study.fb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 *
 * @author jdowling
 */
@ManagedBean(name = "FilesMB")
@SessionScoped
public class FilesMB implements Serializable{

    List<FileInfo> files = new ArrayList<>();
    FileInfo root = new FileInfo("/", "root", FileInfo.Status.READY, 0, true, null);
    FileInfo cwd;


    public FilesMB() {
        this.cwd = this.root;
        this.cwd.addChild(new FileInfo("tmp", "jim", FileInfo.Status.READY, 1024, false, this.cwd));
    }
    
    public List<FileInfo> getFiles() {
        return cwd.getChildren();
    }

    public void setFiles(List<FileInfo> files) {
        this.files = files;
    }
    
    public void cdUp() {
        if (!cwd.isRoot()) {
            // nullify object reference to prevent mem leak
            cwd.setParent(null);
            // set cwd to move up a directory
            cwd = cwd.getParent();
        }
    }
    
    public void cdDown(String name) {

        for (FileInfo f : cwd.getChildren()) {
            if (f.getName().compareTo(name) == 0 && f.isDir()) {
                cwd = f;
            }
        }
    }
    
}
