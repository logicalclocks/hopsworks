/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class NodeStatusTracker implements Runnable {

    private final NodeMetadata launchingNode;
    private final CountDownLatch latch;
    private final CopyOnWriteArraySet<NodeMetadata> pendingNodes;
    private ListenableFuture<ExecResponse> future;
   

    public NodeStatusTracker(NodeMetadata launchingNode, CountDownLatch latch,
            CopyOnWriteArraySet<NodeMetadata> pendingNodes, ListenableFuture<ExecResponse> future
            ) {
        this.launchingNode = launchingNode;
        this.latch = latch;
        this.pendingNodes = pendingNodes;
        this.future = future;
        
    }

    @Override
    public void run() {
        try {
            ExecResponse contents = future.get();
            latch.countDown();
            //...process 
            
            pendingNodes.remove(launchingNode);
        } catch (InterruptedException e) {
            System.out.println("Interrupted" + e);
        } catch (ExecutionException e) {
            System.out.println("Interrupted" + e.getCause());
        }
    }
}
