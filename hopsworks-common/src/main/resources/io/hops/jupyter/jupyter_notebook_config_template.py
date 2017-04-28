#from hdfscontents.hdfsmanager import HDFSContentsManager
c = get_config()
c.HDFSContentsManager.hdfs_namenode_host='%%namenode_ip%%'
c.HDFSContentsManager.hdfs_namenode_port=%%namenode_port%%
c.HDFSContentsManager.root_dir='/Projects/%%project%%/' 
c.HDFSContentsManager.hdfs_user = '%%hdfs_user%%'

c.NotebookApp.contents_manager_class='hdfscontents.hdfsmanager.HDFSContentsManager'

# Set options for certfile, ip, password, and toggle off
# browser auto-opening
#c.NotebookApp.certfile = u'/absolute/path/to/your/certificate/fullchain.pem'
#c.NotebookApp.keyfile = u'/absolute/path/to/your/certificate/privkey.pem'
# Set ip to '*' to bind on all interfaces (ips) for the public server
#c.NotebookApp.ip = '*'
#c.NotebookApp.token = ''
#c.NotebookApp.password = u'%%hashed_password%%'

#c.NotebookApp.ip = '127.0.0.1'


c.NotebookApp.ip = '%%hopsworks_ip%%'
c.NotebookApp.open_browser = False

c.NotebookApp.port_retries = 0
c.NotebookApp.port = %%port%%


c.NotebookApp.tornado_settings = {
    'headers': {
        'Content-Security-Policy': "frame-ancestors 'https://%%hopsworks_ip%%' 'self' "
    }
}

#c.NotebookApp.tornado_settings = {
#    'headers': {
#        'Content-Security-Policy': "frame-ancestors 'ec2-url:9000'",
#    }
#}