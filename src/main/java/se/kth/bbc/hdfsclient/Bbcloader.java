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
import javax.ws.rs.core.MediaType;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import  com.sun.jersey.api.client.config.ClientConfig;
import  com.sun.jersey.api.client.config.DefaultClientConfig;

/**
 * Jersey REST client generated for REST resource:BbcUploader [/bbcuploader]<br>
 * USAGE:
 * <pre>
 *        Bbcloader client = new Bbcloader();
 *        Object response = client.XXX(...);
 *        // do whatever with response
 *        client.close();
 * </pre>
 *
 * @author roshan
 */
public class Bbcloader {
    
    private WebResource webResource;
    private Client client;
    private static final String BASE_URI = "http://snurran.sics.se:8080/hop-dashboard/rest";

    public Bbcloader() {
        
        ClientConfig config = new DefaultClientConfig();
        client = Client.create(config);
        webResource = client.resource(BASE_URI).path("bbcuploader");
        
    }

    public Bbcloader(String username, String password) {
        this();
        setUsernamePassword(username, password);
    }

    //POST
    public ClientResponse uploadFile() throws UniformInterfaceException {
        return webResource.path("upload").accept(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class);
    }

    
    //PUT
    public void putText(Object requestEntity) throws UniformInterfaceException {
        webResource.type(javax.ws.rs.core.MediaType.TEXT_PLAIN).put(requestEntity);
    }

    //GET
    public String downloadFile() throws UniformInterfaceException {
        return webResource.path("read").accept(MediaType.TEXT_PLAIN).get(String.class);
    }

    public void close() {
        client.destroy();
    }

    public final void setUsernamePassword(String username, String password) {
        client.addFilter(new HTTPBasicAuthFilter(username, password));
    }
    
}
