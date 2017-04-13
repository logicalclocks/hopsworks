// Put jupyter in single-tab mode - needed as it is embedded in an iframe
// http://jupyter-notebook.readthedocs.io/en/latest/public_server.html#notebook-server-security
// http://jupyter.readthedocs.io/en/latest/projects/jupyter-directories.html
//
// Start jupyter with JUPYTER_CONFIG_DIR set to the project-specific folder
// JUPYTER_PATH
// Set JUPYTER_RUNTIME_DIR = $JUPYTER_PATH/runtime
define(['base/js/namespace'], function(Jupyter){
    Jupyter._target = '_self';
});