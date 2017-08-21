#from hdfscontents.hdfsmanager import HDFSContentsManager
c = get_config()
c.HDFSContentsManager.hdfs_namenode_host='%%namenode_ip%%'
c.HDFSContentsManager.hdfs_namenode_port=%%namenode_port%%
c.HDFSContentsManager.root_dir='/Projects/%%project%%/' 
c.HDFSContentsManager.hdfs_user = '%%hdfs_user%%'

c.NotebookApp.contents_manager_class='hdfscontents.hdfsmanager.HDFSContentsManager'

#c.NotebookApp.certfile = u'/absolute/path/to/your/certificate/fullchain.pem'
#c.NotebookApp.keyfile = u'/absolute/path/to/your/certificate/privkey.pem'
#c.NotebookApp.token = ''
#c.NotebookApp.password = u'%%hashed_password%%'


#c.NotebookApp.ip = '%%hopsworks_ip%%'
c.NotebookApp.ip = '127.0.0.1'
c.NotebookApp.open_browser = False

c.NotebookApp.port_retries = 0
c.NotebookApp.port = %%port%%

# This is needed for Google Facets
# https://github.com/pair-code/facets
c.NotebookApp.iopub_data_rate_limit=10000000


c.NotebookApp.base_url='/hopsworks-api/jupyter/%%port%%/'

#
# Disable the default Python2 kernel
# https://github.com/jupyter/jupyter_client/issues/144
#
#c.KernelSpecManager.whitelist = set(['Python 2', 'ir'])
#c.KernelSpecManager.whitelist = set(['PySpark', 'ir'])
#c.KernelSpecManager.whitelist = set(['PySpark3', 'ir'])
#c.KernelSpecManager.whitelist = set(['Spark', 'ir'])
#c.KernelSpecManager.whitelist = set(['SparkR', 'ir'])
#c.KernelSpecManager.whitelist = set(['pythonwithpixiedustspark21', 'ir'])
#c.MultiKernelManager.default_kernel_name='PySpark'

c.NotebookApp.allow_origin = '*'

c.NotebookApp.tornado_settings = {
    'headers': {
        'Content-Security-Policy': "child-src * "
#'Content-Security-Policy': "frame-ancestors 'https://%%hopsworks_endpoint%%' 'self' "
    }
}