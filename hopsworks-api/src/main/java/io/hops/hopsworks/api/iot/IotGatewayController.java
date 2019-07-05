package io.hops.hopsworks.api.iot;

import com.google.gson.Gson;
import io.hops.hopsworks.common.dao.iot.IotGatewayConfiguration;
import io.hops.hopsworks.common.dao.iot.IotGatewayFacade;
import io.hops.hopsworks.common.dao.iot.IotGatewayState;
import io.hops.hopsworks.common.dao.iot.IotGateways;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.exceptions.GatewayException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class IotGatewayController {
  
  @EJB
  private IotGatewayFacade iotGatewayFacade;
  
  private static final Logger LOGGER = Logger.getLogger(IotGatewayController.class.getName());
  
  public IotGatewayDetails getGateway(Project project, String name) throws
    GatewayException, URISyntaxException, IOException {
    IotGateways gateway = iotGatewayFacade.findByProjectAndName(project, name);
    if (gateway == null) {
      throw new GatewayException(RESTCodes.GatewayErrorCode.GATEWAY_NOT_FOUND, Level.FINEST, "gatewayName:" + name);
    }
    CloseableHttpClient httpClient = HttpClients.createDefault();
    URI uri = new URIBuilder()
      .setScheme("http")
      .setHost(gateway.getDomain())
      .setPort(gateway.getPort())
      .setPath("/gateway")
      .build();
    HttpGet httpGet = new HttpGet(uri);
    CloseableHttpResponse response = httpClient.execute(httpGet);
    return responseToIotGatewayDetails(response, gateway);
  }
  
  private IotGatewayDetails responseToIotGatewayDetails(CloseableHttpResponse response, IotGateways gateway)
    throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(response.getEntity().getContent(), writer);
    String json = writer.toString();
    
    Gson gson = new Gson();
    IotGatewayDetails gw = gson.fromJson(json, IotGatewayDetails.class);
    gw.setIotGateway(gateway);
    return gw;
  }
  
  public IotGateways putGateway(Project project, IotGatewayConfiguration config) {
    if (project == null || config == null) {
      throw new IllegalArgumentException("Arguments cannot be null.");
    }
  
    IotGateways gateway = iotGatewayFacade.findByProjectAndName(project, config.getName());
  
    if (gateway == null) {
      gateway = new IotGateways(config, project, IotGatewayState.ACTIVE);
    } else if (gateway.getState() == IotGatewayState.INACTIVE_BLOCKED) {
      gateway.setState(IotGatewayState.BLOCKED);
    }
    
    gateway = iotGatewayFacade.putIotGateway(gateway);
    return gateway;
  }
  
  public List<IotDevice> getNodesOfGateway(String gatewayName, Project project)
    throws URISyntaxException, IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    IotGateways gateway = iotGatewayFacade.findByProjectAndName(project, gatewayName);
    URI uri = new URIBuilder()
      .setScheme("http")
      .setHost(gateway.getDomain())
      .setPort(gateway.getPort())
      .setPath("/gateway/nodes")
      .build();
    HttpGet httpGet = new HttpGet(uri);
    CloseableHttpResponse response = httpClient.execute(httpGet);
    return responseToDevices(response, gatewayName);
  }
  
  private List<IotDevice> responseToDevices(CloseableHttpResponse response, String gatewayName)
    throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(response.getEntity().getContent(), writer);
    String json = writer.toString();
    
    Gson gson = new Gson();
    IotDevice[] array = gson.fromJson(json, IotDevice[].class);
    List<IotDevice> list = Arrays.asList(array);
    list.forEach(d -> d.setGatewayName(gatewayName));
    return list;
  }
  
  //TODO: future work - implement in terms of an endpoint that doesn't return all nodes
  public IotDevice getNodeById(String gatewayName, String nodeId, Project project)
    throws URISyntaxException, IOException {
    return getNodesOfGateway(gatewayName, project)
      .stream()
      .filter(d -> d.getEndpoint().equals(nodeId))
      .findAny()
      .orElseThrow(IllegalArgumentException::new);
  }
  
  public void actionBlockingNode(String gatewayName, String nodeId, Project project, Boolean block)
    throws URISyntaxException, IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    IotGateways gateway = iotGatewayFacade.findByProjectAndName(project, gatewayName);
    URI uri = new URIBuilder()
      .setScheme("http")
      .setHost(gateway.getDomain())
      .setPort(gateway.getPort())
      .setPath("/gateway/nodes/" + nodeId + "/blocked")
      .build();
    HttpRequestBase method;
    if (block) {
      method = new HttpPost(uri);
    } else {
      method = new HttpDelete(uri);
    }
    CloseableHttpResponse response = httpClient.execute(method);
  }
  
  public void sendJwtToIotGateway(IotGatewayConfiguration config, Integer projectId, String jwt) throws
    URISyntaxException,
    IOException  {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    URI uri = new URIBuilder()
      .setScheme("http")
      .setHost(config.getDomain())
      .setPort(config.getPort())
      .setPath("/gateway/jwt")
      .build();
    List<NameValuePair> form = new ArrayList<>();
    form.add(new BasicNameValuePair("projectId", Integer.toString(projectId)));
    form.add(new BasicNameValuePair("jwt", jwt));
    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
    
    HttpPost httpPost = new HttpPost(uri);
    httpPost.setEntity(entity);
    CloseableHttpResponse response = httpClient.execute(httpPost);
  }
}
