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


c.NotebookApp.ip = '%%hopsworks_ip%%'
c.NotebookApp.open_browser = False

c.NotebookApp.port_retries = 0
c.NotebookApp.port = %%port%%


c.NotebookApp.allow_origin = '*'

c.NotebookApp.tornado_settings = {
    'headers': {
        'Content-Security-Policy': "child-src * "
    }
}