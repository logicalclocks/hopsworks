require 'airborne'
#require 'byebug'
require 'active_record'
require 'launchy'

require 'dotenv'
Dotenv.load

mysql_adapter = "mysql2"
if RUBY_PLATFORM == "java" 
  mysql_adapter = "jdbcmysql"
end

begin
  ActiveRecord::Base.establish_connection ({
    :adapter => "#{mysql_adapter}",
    :host => ENV['DB_HOST'],
    :port => ENV['DB_PORT'],
    :database => "hopsworks",
    :username => "kthfs",
    :password => "kthfs"})
  ActiveRecord::Base.connection # Calls connection object
    puts "Connected to database!" if ActiveRecord::Base.connected? 
    puts "Not connected to database!" unless ActiveRecord::Base.connected?
  rescue
    puts "Error when connecting to database!"
end

Dir[File.join(File.dirname(__FILE__), 'factories', '**', '*.rb')].each { |f| require f }

Dir[File.join(File.dirname(__FILE__), 'helpers', '**', '*.rb')].each { |f| require f }

RSpec.configure do |config|
  config.include SessionHelper
  config.include ProjectHelper
  config.include FactoryHelper
  config.include DatasetHelper
  #config.before(:suite) { clean_test_data }
  config.after(:suite) { 
    clean_test_data
    if ENV['LAUNCH_BROWSER'] && ENV['LAUNCH_BROWSER']=="true"
       Launchy.open("#{ENV['PROJECT_DIR']}#{ENV['RSPEC_REPORT']}") 
    end  
  }
end

Airborne.configure do |config|
  config.base_url = "http://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
end

def clean_test_data
  puts "Cleaning test data ..."
  require 'net/ssh'
  require 'net/ssh/shell'
  if ENV['RSPEC_SSH'] && ENV['RSPEC_SSH']=="true"
    Net::SSH::start(ENV['RSPEC_SSH_HOST'], "#{ENV['RSPEC_SSH_USER']}") do |ssh|
      ssh.shell do |sh|
        puts "Remote HDFS Clean-up starting"
        sh.execute("cd #{ENV['RSPEC_SSH_USER_DIR']}")
        sh.execute("vagrant ssh -c 'sudo -u glassfish /srv/hops/hadoop/bin/hadoop fs -rm -f -R -skipTrash /Projects ' ") 
        puts "Remote HDFS Clean-up finished"
        
        puts "DataBase Clean-up starting"
        sh.execute("vagrant ssh -c 'sudo /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"DROP DATABASE IF EXISTS hopsworks\" ' ")
        sh.execute("vagrant ssh -c 'sudo /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"CREATE DATABASE IF NOT EXISTS hopsworks CHARACTER SET latin1\" ' ")
        sh.execute("vagrant ssh -c 'sudo cat /srv/hops/tables.sql | sudo /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks' ")
        sh.execute("vagrant ssh -c 'sudo cat /srv/hops/rows.sql | sudo /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks' ")
        sh.execute("vagrant ssh -c 'sudo cat /srv/hops/views.sql | sudo /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks' ")
        res = sh.execute("exit")
        res.on_finish do |val1, val2|
        puts "DB Clean-up finished"
        end
      end
    end
  else
    puts "Vagrant HDFS Clean-up starting"
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo -u glassfish /srv/hops/hadoop/bin/hadoop fs -rm -f -R -skipTrash /Projects ' ") 
    puts "Vagrant HDFS Clean-up finished"
    
    puts "DataBase Clean-up starting"
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"DROP DATABASE IF EXISTS hopsworks\" ' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"CREATE DATABASE IF NOT EXISTS hopsworks CHARACTER SET latin1\" ' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo cat /srv/hops/tables.sql | sudo /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo cat /srv/hops/rows.sql | sudo /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo cat /srv/hops/views.sql | sudo /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks' ")
    puts "DB Clean-up finished" 
  end  
end
