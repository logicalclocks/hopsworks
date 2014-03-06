/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.deploy.virtualization;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.http.HttpResponseException;
import se.kth.hop.deploy.provision.MessageController;
import se.kth.hop.deploy.virtualization.parser.NodeGroup;

/**
 * Asynchronous Thread that will send a createNodesInGroup request to the cloud 
 * api through Jclouds compute service abstraction.
 * 
 * If we get a HTTPResponse Exception, we have passed the request limit, we need to retry again those
 * nodes.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class CreateGroupCallable implements Callable<Set<? extends NodeMetadata>> {

    private ComputeService service;
    private NodeGroup group;
    private TemplateBuilder kthfsTemplate;
    private Map<String, Set<? extends NodeMetadata>> nodes;
    private MessageController messages;
    private CopyOnWriteArraySet<NodeGroup> pending;

    public CreateGroupCallable(ComputeService service, NodeGroup group,
            TemplateBuilder kthfsTemplate, Map<String, Set<? extends NodeMetadata>> nodes,
            MessageController messages, CopyOnWriteArraySet<NodeGroup> pending) {
        this.service = service;
        this.group = group;
        this.kthfsTemplate = kthfsTemplate;
        this.nodes = nodes;
        this.messages = messages;
        this.pending = pending;
    }

    @Override
    public Set<? extends NodeMetadata> call() {
        Set<? extends NodeMetadata> ready = null;
        try {
            ready = service.createNodesInGroup(group.getServices().get(0), group.getNumber(),
                    kthfsTemplate.build());
            nodes.put(group.getServices().get(0), ready);
            messages.addMessage("Nodes created in Security Group " + group.getServices().get(0) + " with "
                    + "basic setup");
        } catch (HttpResponseException e) {
            //We have overpassed the limit of the API, need to mark this group as inconsistent
            System.out.println(e);
            pending.add(group);
        } catch (RunNodesException e) {
            System.out.println("error adding nodes to group "
                    + "ups something got wrong on the nodes");
        } finally {

            return ready;
        }
    }
}
