/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.hdfsclient;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;


/**
 *
 * @author roshan
 */
@ManagedBean
@RequestScoped
public class WebHDFSClient {
    
    private WebResource webResource;
    private Client client;
    private String HDFS_HTTP_URL;
    
    @PostConstruct
    protected void init(){
    
        FacesContext context = FacesContext.getCurrentInstance();
        HDFS_HTTP_URL = context.getExternalContext().getInitParameter("Webhdfs-http");
    
        ClientConfig config = new DefaultClientConfig();
        client = Client.create(config);
        
      
    }
    
    
    public WebResource getWebResource(String relativeUrl){
    
        if(client == null)
            init();
        
        return client.resource(HDFS_HTTP_URL+relativeUrl);
    }
    
     public ClientResponse clientGetResponse(String relativeUrl) throws UniformInterfaceException {
            
            return webResource.accept("application/json").get(ClientResponse.class);
     }
     
     
}
