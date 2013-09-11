/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import se.kth.kthfsdashboard.provision.MessageController;
import se.kth.kthfsdashboard.virtualization.clusterparser.NodeGroup;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class CreateGroupCallable implements Callable<Set<? extends NodeMetadata>> {

    private ComputeService service;
    private NodeGroup group;
    private TemplateBuilder kthfsTemplate;
    private Map<String, Set<? extends NodeMetadata>> nodes;
    private MessageController messages;

    public CreateGroupCallable(ComputeService service, NodeGroup group,
            TemplateBuilder kthfsTemplate, Map<String, Set<? extends NodeMetadata>> nodes,
            MessageController messages) {
        this.service = service;
        this.group = group;
        this.kthfsTemplate = kthfsTemplate;
        this.nodes = nodes;
        this.messages = messages;
    }

    @Override
    public Set<? extends NodeMetadata> call() {
        Set<? extends NodeMetadata> ready = null;
        try {
            ready = service.createNodesInGroup(group.getSecurityGroup(), group.getNumber(),
                    kthfsTemplate.build());
            nodes.put(group.getSecurityGroup(), ready);
            messages.addMessage("Nodes created in Security Group " + group.getSecurityGroup() + " with "
                    + "basic setup");
        } catch (RunNodesException e) {
            System.out.println("error adding nodes to group "
                    + "ups something got wrong on the nodes");
        } finally {

            return ready;
        }
    }
}
