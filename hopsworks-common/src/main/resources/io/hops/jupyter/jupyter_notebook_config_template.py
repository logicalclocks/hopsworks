c = get_config()
c.HDFSContentsManager.hdfs_namenode_host='%%namenode_ip%%'
c.HDFSContentsManager.hdfs_namenode_port=%%namenode_port%%
c.HDFSContentsManager.root_dir='/Projects/%%project%%%%base_dir%%' 
c.HDFSContentsManager.hdfs_user = '%%hdfs_user%%'
c.HDFSContentsManager.hadoop_client_env_opts = '-D fs.permissions.umask-mode=0002'

c.NotebookApp.contents_manager_class='hdfscontents.hdfsmanager.HDFSContentsManager'

c.NotebookApp.ip = '127.0.0.1'
c.NotebookApp.open_browser = False

c.NotebookApp.notebook_dir = '%%secret_dir%%'

c.NotebookApp.port_retries = 0
c.NotebookApp.port = %%port%%

# This is needed for Google Facets
# https://github.com/pair-code/facets
c.NotebookApp.iopub_data_rate_limit=10000000

c.NotebookApp.base_url='/hopsworks-api/jupyter/%%port%%/'
c.Application.log_level="WARN"
c.JupyterConsoleApp.kernel_name="PySpark"

c.KernelSpecManager.whitelist = {'pysparkkernel', 'sparkkernel', 'sparkrkernel','pythonwithpixiedustspark22'
%%python-kernel%% }
c.KernelSpecMAnager.ensure_native_kernel=False

#Available kernels:
#  sparkkernel                   /usr/local/share/jupyter/kernels/sparkkernel
#  pysparkkernel                 /usr/local/share/jupyter/kernels/pysparkkernel
#  pyspark3kernel                /usr/local/share/jupyter/kernels/pyspark3kernel
#  sparkrkernel                  /usr/local/share/jupyter/kernels/sparkrkernel
#  python2                       /usr/local/share/jupyter/kernels/python-kernel

c.NotebookApp.allow_origin = '*'
c.NotebookApp.tornado_settings = {
    'headers': {
        'Content-Security-Policy': "frame-ancestors 'self' "
    }
}

