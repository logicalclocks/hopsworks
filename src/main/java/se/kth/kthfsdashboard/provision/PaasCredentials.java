/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@Entity
@Table(name = "PaasCredentials")
@NamedQueries({
    @NamedQuery(name = "PaasCredentials.findAll", query = "SELECT c FROM PaasCredentials c")
})
public class PaasCredentials implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String provider;
    private String email;
    private String dashboardIP;
    @Column(columnDefinition="text")
    private String publicKey;
    @Column(columnDefinition="text")
    private String privateKey;
    private String accountId;
    private String accessKey;
    private String keystoneURL;

    public PaasCredentials() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDashboardIP() {
        return dashboardIP;
    }

    public void setDashboardIP(String dashboardIP) {
        this.dashboardIP = dashboardIP;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getKeystoneURL() {
        return keystoneURL;
    }

    public void setKeystoneURL(String keystoneURL) {
        this.keystoneURL = keystoneURL;
    }
    
}
