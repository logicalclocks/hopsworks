/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.job;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Date;
import java.util.concurrent.Executors;
import se.kth.kthfsdashboard.wf.Workflow;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */

//@ManagedBean
//@SessionScoped
public class JobDispatcher{
    
    private ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
    
    private JobHistoryFacade history;

    public JobDispatcher() {
    }
    
    public JobDispatcher(JobHistoryFacade jobs){
        this.history= jobs;
    }
    
    public void submitWorkflowTask(Workflow selectedWorkflow){
        
        //We would create a new entry in the history table with the state of the running job to pending
        Date actual = new Date();
        
        //submit the job to the pool and attach a listening Thread to update the entry
        ListenableFuture<Job>  jobTask =pool.submit(new WorkflowJobTask(selectedWorkflow, history));
        jobTask.addListener(new JobTracker(jobTask,history), pool);
    }
    
}
