/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.shell;

import java.io.File;
import java.util.List;

/**
 * See TaskEntity.java and TaskManager.java from Serengeti.
 * @author jdowling
 */
public class ExecProcess implements Runnable {

    private volatile boolean finished;
    private final List<String> cmds;
    private String res;
    private File workingDir;

    public ExecProcess(List<String> cmds, File workingDir) {
        this.cmds = cmds;
        finished = false;
        this.workingDir = workingDir;
    }

    public void run() {

		try {
			Process process = 
                                new ProcessBuilder(cmds).directory(workingDir).
                                redirectErrorStream(true).start();

//      Thread stdoutReaperThread = new Thread(new StreamReaper(proc.getInputStream(),
//            new File(taskEntity.getWorkDir(), TaskManager.STDOUT_FILENAME)));
//      Thread stderrReaperThread = new Thread(new StreamReaper(proc.getErrorStream(),
//            new File(taskEntity.getWorkDir(), TaskManager.STDERR_FILENAME)));                        
                        int code = process.waitFor();
			res = "Command executed with exit code - " + code;
		} catch (Exception ex) {
			res =  "Command execution failed - " + ex;
		} finally {
		}
        
    }

    public boolean isComplete() {
        return finished;
    }
    
}
