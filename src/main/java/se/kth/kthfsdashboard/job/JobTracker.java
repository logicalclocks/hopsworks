/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.job;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class JobTracker  implements Runnable {

    private ListenableFuture<Job> futureJobResult;

    public JobTracker(ListenableFuture<Job> futureJobResult) {
        this.futureJobResult = futureJobResult;
    }
    
    
    @Override
    public void run() {
        try {
            //Submit job result to the database so we can fetch the history.
            System.out.println(futureJobResult.get().getName());
        } catch (InterruptedException ex) {
            System.out.println("Thread Interrupted!");
        } catch (ExecutionException ex) {
            System.out.println("Failed Execution");
        }
    }
    
    
    
}
