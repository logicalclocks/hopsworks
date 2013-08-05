/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import java.util.concurrent.Callable;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class InstallBaremetalCallable implements Callable<ExecResponse> {

    private NodeMetadata node;
    private JHDFSScriptBuilder script;

    public InstallBaremetalCallable(NodeMetadata node, JHDFSScriptBuilder script) {
        this.node = node;
        this.script = script;
    }

    
    @Override
    public ExecResponse call() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
