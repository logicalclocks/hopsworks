/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.deploy.virtualization;

import com.google.common.base.Predicates;
import static com.google.common.base.Predicates.not;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jclouds.aws.AWSResponseException;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.TemplateBuilder;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;
import se.kth.hop.deploy.provision.MessageController;
import se.kth.hop.deploy.virtualization.parser.NodeGroup;

/**
 * Asynchronous Thread that will send a purge request to remove phantomNodes from
 * a previous failed creation of nodes. The next part we do a createNodesInGroup request to the 
 * cloud api through Jclouds compute service abstraction. 
 * 
 * If we get a HTTPResponse Exception, we have passed the request limit, we need to retry again those
 * nodes.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class PurgeCreateGroupCallable implements Callable<Set<? extends NodeMetadata>> {

    private ComputeService service;
    private NodeGroup group;
    private TemplateBuilder kthfsTemplate;
    private Map<String, Set<? extends NodeMetadata>> nodes;
    private MessageController messages;
    private CopyOnWriteArraySet<NodeGroup> pending;

    public PurgeCreateGroupCallable(ComputeService service, NodeGroup group,
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
    public Set<? extends NodeMetadata> call() throws Exception {
        
        Set<? extends NodeMetadata> ready = null;
        try {
            //first purge the security group
            service.destroyNodesMatching(Predicates.<NodeMetadata> and(not(TERMINATED), 
                    inGroup(group.getServices().get(0))));
            //create again
            ready = service.createNodesInGroup(group.getServices().get(0), group.getNumber(),
                    kthfsTemplate.build());
            nodes.put(group.getServices().get(0), ready);
            messages.addMessage("Nodes created in Security Group " + group.getServices().get(0) + " with "
                    + "basic setup");
            pending.remove(group);
        } catch (AWSResponseException e) {
            //We have overpassed the limit of the API, need to mark this group as inconsistent still
            //this will update its entry
            //pending.add(group); do not do anything really
        } catch (RunNodesException e) {
            System.out.println("error adding nodes to group "
                    + "ups something got wrong on the nodes");
        } finally {

            return ready;
        }
    }
}
