require 'airborne'
#require 'byebug'
require 'active_record'

require 'dotenv'
Dotenv.load

ActiveRecord::Base.establish_connection ({
  :adapter => "jdbcmysql",
  :host => ENV['DB_HOST'],
  :port => ENV['DB_PORT'],
  :database => "hopsworks",
  :username => "kthfs",
  :password => "kthfs"})

Dir[File.join(File.dirname(__FILE__), 'factories', '**', '*.rb')].each { |f| require f }

Dir[File.join(File.dirname(__FILE__), 'helpers', '**', '*.rb')].each { |f| require f }

RSpec.configure do |config|
  config.include SessionHelper
  config.include ProjectHelper
  config.include WorkflowHelper
  config.include FactoryHelper
  config.before(:suite) do
    #clean_oozie
  end
  config.after(:suite) { clean_test_data }
end

Airborne.configure do |config|
  config.base_url = "http://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
end

def clean_oozie
  puts "Killing oozie processes"
  uri = URI "http://#{ENV['OOZIE_HOST']}/oozie/v1/jobs?action=kill&filter=user%3Dglassfish&jobtype=wf"
  req = Net::HTTP::Put.new(uri)
  res = Net::HTTP.start(uri.hostname, uri.port){|http| http.request(req)}
  puts res.code
  puts "Killed oozie processes"
end

def clean_test_data
  puts "Cleaning test data ..."
  require 'net/ssh'
  require 'net/ssh/shell'
  if ENV['RSPEC_SSH'] && ENV['RSPEC_SSH']=="true"
    Net::SSH::start(ENV['RSPEC_SSH_HOST'], 'root') do |ssh|
      ssh.shell do |sh|
        puts "Remote HDFS Clean-up starting"
        sh.execute("cd #{ENV['RSPEC_SSH_USER_DIR']}")
        sh.execute("vagrant ssh -c 'sudo -u glassfish /srv/hadoop/bin/hadoop fs -rm -R /Projects/project_* ' ") 
        puts "Remote HDFS Clean-up finished"
#        sh.execute("vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"DROP DATABASE IF EXISTS oozie\" ' ")
#        sh.execute("vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"CREATE DATABASE IF NOT EXISTS oozie CHARACTER SET latin1\" ' ")
#        sh.execute("vagrant ssh -c 'sudo cat /srv/oozie/oozie.sql | sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh --database=oozie' ")
        res =sh.execute("exit")
        res.on_finish do |val1, val2|
        
        end
      end
    end
  else
    puts "Vagrant HDFS Clean-up starting"
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo -u glassfish /srv/hadoop/bin/hadoop fs -rm -R /Projects/project_* ' ") 
    puts "Vagrant HDFS Clean-up finished"
#    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"DROP DATABASE IF EXISTS oozie\" ' ")
#    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"CREATE DATABASE IF NOT EXISTS oozie CHARACTER SET latin1\" ' ")
#    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo cat /srv/oozie/oozie.sql | sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh --database=oozie' ")
  end
  puts "DataBase Clean-up starting"
  ActiveRecord::Base.connection.execute("DELETE FROM hopsworks.project_payments_history WHERE username LIKE \'%@email.com\' ")
  ActiveRecord::Base.connection.execute("DELETE FROM hopsworks.account_audit ")
  ActiveRecord::Base.connection.execute("DELETE FROM hopsworks.users WHERE fname= \'name\' ")
  puts "DB Clean-up finished"
end
