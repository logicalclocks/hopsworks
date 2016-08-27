package se.kth.bbc.jobs.jobhistory;

import java.util.Comparator;

public class JobHeuristicDetailsComparator implements Comparator<JobHeuristicDetailsDTO>{

    @Override
    public int compare(JobHeuristicDetailsDTO t, JobHeuristicDetailsDTO t1) {
        
        return Long.compare(t.getExecutionTime(),t1.getExecutionTime());
    
    }
    
}
