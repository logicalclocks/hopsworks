from hdfscontents import HdfsContentsManager
c = get_config()
c.NotebookApp.contents_manager_class = HdfsContentsManager

# Set options for certfile, ip, password, and toggle off
# browser auto-opening
#c.NotebookApp.certfile = u'/absolute/path/to/your/certificate/fullchain.pem'
#c.NotebookApp.keyfile = u'/absolute/path/to/your/certificate/privkey.pem'
# Set ip to '*' to bind on all interfaces (ips) for the public server
#c.NotebookApp.ip = '*'
#c.NotebookApp.token = ''
#c.NotebookApp.password = u'%%hashed_password%%'

#c.NotebookApp.ip = '127.0.0.1'
c.NotebookApp.ip = 'localhost'
c.NotebookApp.open_browser = False

# It is a good idea to set a known, fixed port for server access
c.NotebookApp.port = %%jupyter_port%%

c.HdfsContentsManager.user_id = '%%hdfs_user%%'
c.HdfsContentsManager.hadoop_home = '%%hadoop_home%%'


c.NotebookApp.tornado_settings = {
    'headers': {
        'Content-Security-Policy': "frame-ancestors 'https://%%hopsworks_ip%%' 'self' "
    }
}