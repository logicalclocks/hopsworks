/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.wf.job;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class JobTracker  implements Runnable {

    private ListenableFuture<Job> futureJobResult;
    private JobHistoryFacade history;

    public JobTracker(ListenableFuture<Job> futureJobResult, JobHistoryFacade history) {
        this.history = history;
        this.futureJobResult = futureJobResult;
    }
    
    
    @Override
    public void run() {
        try {
            //Submit job result to the database so we can fetch the history.
            Job result = futureJobResult.get();
            history.edit(result);
            System.out.println(result.getName());
        } catch (InterruptedException ex) {
            System.out.println("Thread Interrupted!");
        } catch (ExecutionException ex) {
            System.out.println("Failed Execution");
        }
    }
    
    
    
}
