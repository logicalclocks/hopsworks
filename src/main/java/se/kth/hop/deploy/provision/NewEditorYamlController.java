/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.deploy.provision;

import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.hop.deploy.virtualization.parser.ClusterEntity;
import se.kth.hop.deploy.virtualization.parser.ClusterFacade;

/**
 * Controller that handles edition of YAML cluster files using a code console
 * interface.
 * 
 * We should be doing the following:
 * http://leonotepad.blogspot.se/2014/01/playing-with-antlr4-primefaces.html
 *
 * @author Jim
 */
@ManagedBean
@ViewScoped
public class NewEditorYamlController implements Serializable {

    private static final long serialVersionUID = 20131126L;
    Logger logger = LoggerFactory.getLogger(NewEditorYamlController.class);

    private String content;
    private String mode = "yaml";

    public NewEditorYamlController() {
    }

    public void init() {
//        this.content = getOneNodeCluster();
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(final String mode) {
        this.mode = mode;
    }


    public void invokeJclouds() {
//        return "editcluster";
    }

    public String getOneNodeCluster() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("!!se.kth.hop.deploy.virtualization.parser.Cluster \n "
                + "name: test2 \n  installPhase: true\n   global:\n     recipes:\n   - ssh\n - chefClient "
                + "authorizePorts:  \n      - 3306 \n     - 4343  \n     - 3321 \n    git: \n"
                + "      user: Jim Dowling \n      repository: https://github.com/hopstart/hop-chef.git \n"
                + "      key: notNull \n  provider: \n    name: aws-ec2 \n    instanceType: m1.large \n"
                + "    loginUser: ubuntu \n    image: eu-west-1/ami-ffcdce8b \n    region: eu-west-1 \n"
                + "  NodeGroup: \n  - service: ndb \n    number: 2 \n  - service: mgm \n    number: 1 \n"
                + "  - service: mysqld \n    number: 1 \n    recipes:  \n      - namenode \n  - service: namenode \n"
                + "    number: 1 \n    recipes: \n      - resourcemanager \n  - service: datanode \n"
                + "    number: 2 \n    recipes: \n      - nodemanager \n... \n"
        );
        setContent(sb.toString());
        return sb.toString();
    }

    public String getThreeNodeCluster() {
        StringBuilder sb = new StringBuilder();
        sb.append("!!se.kth.hop.deploy.virtualization.parser.Cluster \n "
                + "name: test2 \n  installPhase: true\n   global:\n     recipes:\n   - ssh\n - chefClient "
                + "authorizePorts:  \n      - 3306 \n     - 4343  \n     - 3321 \n    git: \n"
                + "      user: Jim Dowling \n      repository: https://github.com/hopstart/hop-chef.git \n"
                + "      key: notNull \n  provider: \n    name: aws-ec2 \n    instanceType: m1.large \n"
                + "    loginUser: ubuntu \n    image: eu-west-1/ami-ffcdce8b \n    region: eu-west-1 \n"
                + "  NodeGroup: \n  - service: ndb \n    number: 2 \n  - service: mgm \n    number: 1 \n"
                + "  - service: mysqld \n    number: 1 \n    recipes:  \n      - namenode \n  - service: namenode \n"
                + "    number: 1 \n    recipes: \n      - resourcemanager \n  - service: datanode \n"
                + "    number: 2 \n    recipes: \n      - nodemanager \n... \n"
        );

        setContent(sb.toString());
        return sb.toString();

    }

    public String getFiveNodeCluster() {
        StringBuilder sb = new StringBuilder();
        sb.append("!!se.kth.hop.deploy.virtualization.parser.Cluster \n "
                + "name: test2 \n  installPhase: true\n   global:\n     recipes:\n   - ssh\n - chefClient "
                + "authorizePorts:  \n      - 3306 \n     - 4343  \n     - 3321 \n    git: \n"
                + "      user: Jim Dowling \n      repository: https://github.com/hopstart/hop-chef.git \n"
                + "      key: notNull \n  provider: \n    name: aws-ec2 \n    instanceType: m1.large \n"
                + "    loginUser: ubuntu \n    image: eu-west-1/ami-ffcdce8b \n    region: eu-west-1 \n"
                + "  NodeGroup: \n  - service: ndb \n    number: 2 \n  - service: mgm \n    number: 1 \n"
                + "  - service: mysqld \n    number: 1 \n    recipes:  \n      - namenode \n  - service: namenode \n"
                + "    number: 1 \n    recipes: \n      - resourcemanager \n  - service: datanode \n"
                + "    number: 2 \n    recipes: \n      - nodemanager \n... \n"
        );
        
        
        setContent(sb.toString());
        return sb.toString();

    }
}
