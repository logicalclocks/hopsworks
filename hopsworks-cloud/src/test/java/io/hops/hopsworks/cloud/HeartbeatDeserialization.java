/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.hops.hopsworks.cloud.dao.heartbeat.HeartbeatRequest;
import io.hops.hopsworks.cloud.dao.heartbeat.HeartbeatResponse;
import io.hops.hopsworks.cloud.dao.heartbeat.HeartbeatResponseHttpMessage;
import io.hops.hopsworks.cloud.dao.heartbeat.RegistrationRequest;
import io.hops.hopsworks.cloud.dao.heartbeat.RegistrationResponse;
import io.hops.hopsworks.cloud.dao.heartbeat.Version;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommand;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommandType;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommandTypeDeserializer;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommandsDeserializer;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CommandStatus;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.RemoveNodesCommand;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.RemoveNodesCommandSerializer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeartbeatDeserialization {
  private static Gson gson;

  @BeforeClass
  public static void beforeAll() {
    gson = new GsonBuilder()
            .registerTypeAdapter(RemoveNodesCommand.class, new RemoveNodesCommandSerializer())
            .registerTypeAdapter(CloudCommand.class, new CloudCommandsDeserializer())
            .registerTypeAdapter(CloudCommandType.class, new CloudCommandTypeDeserializer())
            .create();
  }

  @Test
  public void testRegistrationRequest() {
    RegistrationRequest rr = constructRegistrationRequest();
    String rrJson = gson.toJson(rr);

    RegistrationRequest rrDes = gson.fromJson(rrJson, RegistrationRequest.class);
    Assert.assertEquals(rrJson, gson.toJson(rrDes));
  }

  @Test
  public void testRegistrationResponse() {
    RegistrationResponse rr = constructRegistrationResponse();
    String rrJson = gson.toJson(rr);

    RegistrationResponse rrDes = gson.fromJson(rrJson, RegistrationResponse.class);
    String rrDesJson = gson.toJson(rrDes);
    Assert.assertEquals(rrJson, rrDesJson);
  }

  @Test
  public void testHeartbeatRequest() {
    HeartbeatRequest hr = constructHeartbeatRequest();
    assertHeartbeatRequest(hr);
  }

  @Test
  public void testEmptyHeartbeatRequest() {
    HeartbeatRequest ehr = constructEmptyHeartbeatRequest();
    assertHeartbeatRequest(ehr);
  }

  @Test
  public void testHeartbeatResponse() {
    Gson gson = new GsonBuilder()
            .registerTypeAdapter(RemoveNodesCommand.class, new RemoveNodesCommandSerializer())
            .registerTypeAdapter(DummyCommand.class, new DummyCommandSerializer())
            .registerTypeAdapter(CloudCommand.class, new TestCloudCommandsDeserializer())
            .registerTypeAdapter(CloudCommandType.class, new CloudCommandTypeDeserializer())
            .create();

    HeartbeatResponse hr = constructHeartbeatResponse();
    assertHeartbeatResponse(hr, gson);

    hr = constructEmptyHeartbeatResponse();
    assertHeartbeatResponse(hr, gson);

    hr = constructWorkersHeartbeatResponse();
    assertHeartbeatResponse(hr, gson);
  }

  @Test
  public void testHttpMessageHeartbeatResponse() {
    Gson gson = new GsonBuilder()
            .registerTypeAdapter(RemoveNodesCommand.class, new RemoveNodesCommandSerializer())
            .registerTypeAdapter(DummyCommand.class, new DummyCommandSerializer())
            .registerTypeAdapter(CloudCommand.class, new TestCloudCommandsDeserializer())
            .registerTypeAdapter(CloudCommandType.class, new CloudCommandTypeDeserializer())
            .create();

    HeartbeatResponse hr = constructHeartbeatResponse();
    HeartbeatResponseHttpMessage message = new HeartbeatResponseHttpMessage(200, "OK", "", hr);
    assertHeartbeatResponseHttpMessage(message, gson);


    hr = constructEmptyHeartbeatResponse();
    message = new HeartbeatResponseHttpMessage(200, "OK", "", hr);
    assertHeartbeatResponseHttpMessage(message, gson);

    hr = constructWorkersHeartbeatResponse();
    message = new HeartbeatResponseHttpMessage(200, "OK", "", hr);
    assertHeartbeatResponseHttpMessage(message, gson);
  }

  private void assertHeartbeatResponse(HeartbeatResponse original, Gson gson) {
    String hrJson = gson.toJson(original);
    HeartbeatResponse hrDes = gson.fromJson(hrJson, HeartbeatResponse.class);
    String hrDesJson = gson.toJson(hrDes);
    Assert.assertEquals(hrJson, hrDesJson);
  }

  private void assertHeartbeatRequest(HeartbeatRequest original) {
    String hrJson = gson.toJson(original);
    HeartbeatRequest hrDes = gson.fromJson(hrJson, HeartbeatRequest.class);
    String hrDesJson = gson.toJson(hrDes);
    Assert.assertEquals(hrJson, hrDesJson);
  }

  private void assertHeartbeatResponseHttpMessage(HeartbeatResponseHttpMessage message, Gson gson) {
    String messageJson = gson.toJson(message);
    HeartbeatResponseHttpMessage messageDes = gson.fromJson(messageJson, HeartbeatResponseHttpMessage.class);
    String messageDesJson = gson.toJson(messageDes);
    Assert.assertEquals(messageJson, messageDesJson);
  }

  private RegistrationRequest constructRegistrationRequest() {
    final RegistrationRequest rr = new RegistrationRequest();
    rr.setVersion(Version.V010);
    return rr;
  }

  private RegistrationResponse constructRegistrationResponse() {
    Map<String, Integer> nodesToRemove0 = new HashMap<>();
    nodesToRemove0.put("instance.type.20", 3);
    nodesToRemove0.put("instance.type.10", 2);
    CloudCommand command0 = new RemoveNodesCommand(1L, nodesToRemove0);

    Map<String, Integer> nodesToRemove1 = new HashMap<>();
    nodesToRemove1.put("instance.type.40", 1);
    nodesToRemove1.put("instance.type.30", 6);
    CloudCommand command1 = new RemoveNodesCommand(2L, nodesToRemove1);
    List<CloudCommand> commands = new ArrayList<>(2);
    commands.add(command0);
    commands.add(command1);

    final RegistrationResponse rr = new RegistrationResponse(commands);
    rr.setVersion(Version.V010);
    return rr;
  }

  private HeartbeatRequest constructHeartbeatRequest() {
    List<CloudNode> decommissioningNodes = new ArrayList<>(2);
    decommissioningNodes.add(new CloudNode("node0", "host0", "ip", 0, "instanceType"));
    decommissioningNodes.add(new CloudNode("node1", "host1", "ip", 2, "instanceType1"));

    List<CloudNode> decommissionedNodes = new ArrayList<>(2);
    decommissionedNodes.add(new CloudNode("node2", "host23", "ip", 0, "instanceType"));
    decommissionedNodes.add(new CloudNode("node3", "host24", "ip2", 5, "instanceType5"));

    Map<Long, CommandStatus> commandsStatus = new HashMap<>(2);
    commandsStatus.put(1L, new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.NEW, "message"));
    commandsStatus.put(2L, new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.ONGOING, "message1"));

    return constructHeartbeatRequest(decommissioningNodes, decommissionedNodes, commandsStatus);
  }

  private HeartbeatRequest constructEmptyHeartbeatRequest() {
    return constructHeartbeatRequest(Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_MAP);
  }

  private HeartbeatRequest constructHeartbeatRequest(List<CloudNode> ingNodes, List<CloudNode> edNodes,
          Map<Long, CommandStatus> cst) {
    final HeartbeatRequest hr = new HeartbeatRequest(ingNodes, edNodes, cst);
    hr.setVersion(Version.V010);
    return hr;
  }

  private HeartbeatResponse constructHeartbeatResponse() {
    List<CloudNode> workers = new ArrayList<>(2);
    workers.add(new CloudNode("node0", "host0", "ip", 0, "instanceType"));
    workers.add(new CloudNode("node1", "host1", "ip", 2, "instanceType1"));

    Map<String, Integer> nodesToRemove0 = new HashMap<>();
    nodesToRemove0.put("instance.type.20", 3);
    nodesToRemove0.put("instance.type.10", 2);
    CloudCommand command0 = new RemoveNodesCommand(1L, nodesToRemove0);

    Map<String, Integer> nodesToRemove1 = new HashMap<>();
    nodesToRemove1.put("instance.type.40", 1);
    nodesToRemove1.put("instance.type.30", 6);
    CloudCommand command1 = new RemoveNodesCommand(2L, nodesToRemove1);

    List<CloudCommand> commands = new ArrayList<>(3);
    commands.add(command0);
    commands.add(command1);
    commands.add(new DummyCommand(12L, "args1"));
    commands.add(new DummyCommand(13L, "args2"));

    return constructHeartbeatResponse(workers, commands);
  }

  private HeartbeatResponse constructEmptyHeartbeatResponse() {
    final HeartbeatResponse hr = new HeartbeatResponse(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    hr.setVersion(Version.V010);
    return hr;
  }

  private HeartbeatResponse constructWorkersHeartbeatResponse() {
    List<CloudNode> workers = new ArrayList<>(2);
    workers.add(new CloudNode("node0", "host0", "ip", 0, "instanceType"));
    workers.add(new CloudNode("node1", "host1", "ip", 2, "instanceType1"));

    final HeartbeatResponse hr = new HeartbeatResponse(workers, Collections.EMPTY_LIST);
    hr.setVersion(Version.V010);
    return hr;
  }

  private HeartbeatResponse constructHeartbeatResponse(List<CloudNode> workers, List<CloudCommand> commands) {
    final HeartbeatResponse hr = new HeartbeatResponse(workers, commands);
    hr.setVersion(Version.V010);
    return hr;
  }
}
