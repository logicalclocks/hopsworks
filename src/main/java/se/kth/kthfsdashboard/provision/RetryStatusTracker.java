/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.jclouds.compute.domain.ExecResponse;

/**
 * Thread that handles the retry call to one of the nodes, it listens waiting for the future 
 * to return the status of the SSH session.
 * 
 * If the script executed in the session returns no errors, we indicate that the node recovered by
 * changing the status into COMPLETE
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class RetryStatusTracker implements Runnable{
     private ListenableFuture<ExecResponse> future;
     private NodeProgression retryNode;
     private DeploymentProgressFacade progress;

    public RetryStatusTracker(ListenableFuture<ExecResponse> future, NodeProgression retryNode,
            DeploymentProgressFacade progress) {
        this.future = future;
        this.retryNode = retryNode;
        this.progress = progress;
    }

    @Override
    public void run() {
        try {
            ExecResponse contents = future.get();
            System.out.println(contents.getExitStatus());

            Integer exitStatus = contents.getExitStatus();
            if (exitStatus==null||exitStatus<1) {

                progress.updatePhaseProgress(retryNode, DeploymentPhase.COMPLETE);
                System.out.println("Retry executed succesfully");
            }

            //...process ssh


        } catch (InterruptedException e) {
            System.out.println("Interrupted" + e);
        } catch (ExecutionException e) {
            System.out.println("Interrupted" + e.getCause());
        } catch (Exception e){
            System.out.println("Error updating node");
        }
    }
     
     
    
}
