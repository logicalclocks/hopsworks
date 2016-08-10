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
    #clean_database
  end
  #config.after(:all) { clean_database }
end

Airborne.configure do |config|
  config.base_url = "http://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
  config.include WorkflowHelper
  config.include FactoryHelper
  # config.before(:suite) do
  #   clean_oozie
  #   clean_database
  # end
  # config.after(:all) { clean_database }
end

Airborne.configure do |config|
  config.base_url = 'http://bbc1.sics.se:14007'
  config.headers = { content_type: 'application/json' }
end

def clean_oozie
  puts "Killing oozie processes"
  uri = URI "http://#{ENV['OOZIE_HOST']}/oozie/v1/jobs?action=kill&filter=user%3Dglassfish&jobtype=wf"
  req = Net::HTTP::Put.new(uri)
  res = Net::HTTP.start(uri.hostname, uri.port){|http| http.request(req)}
  puts res.code
  puts "Killed oozie processes"
end

def clean_database
  require 'net/ssh'
  require 'net/ssh/shell'
  if ENV['RSPEC_SSH'] && ENV['RSPEC_SSH']=="true"
    Net::SSH::start(ENV['RSPEC_SSH_HOST'], 'root') do |ssh|
      ssh.shell do |sh|
        puts "Remote Database Cleaning begining"
        sh.execute("cd #{ENV['RSPEC_SSH_USER_DIR']}")
        sh.execute("vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"DROP DATABASE IF EXISTS oozie\" ' ")
        sh.execute("vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"DROP DATABASE IF EXISTS hopsworks\" ' ")
        sh.execute("vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"CREATE DATABASE IF NOT EXISTS hopsworks CHARACTER SET latin1\" ' ")
        sh.execute("vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"CREATE DATABASE IF NOT EXISTS oozie CHARACTER SET latin1\" ' ")
        sh.execute("vagrant ssh -c 'sudo cat /srv/glassfish/tables.sql | sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks' ")
        sh.execute("vagrant ssh -c 'sudo cat /srv/glassfish/rows.sql | sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks' ")
        sh.execute("vagrant ssh -c 'sudo cat /srv/oozie/oozie.sql | sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh --database=oozie' ")
        res =sh.execute("exit")
        res.on_finish do |val1, val2|
          puts "Remote Database Cleaning finished"
        end
      end
    end
  else
    puts "Vagrant Database Cleaning begining"
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"DROP DATABASE IF EXISTS oozie\" ' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"DROP DATABASE IF EXISTS hopsworks\" ' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"CREATE DATABASE IF NOT EXISTS hopsworks CHARACTER SET latin1\" ' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"CREATE DATABASE IF NOT EXISTS oozie CHARACTER SET latin1\" ' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo cat /srv/glassfish/tables.sql | sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo cat /srv/glassfish/rows.sql | sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks' ")
    # sh.execute("vagrant ssh -c 'sudo cat /srv/oozie/oozie.sql | sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh --database=oozie' ")
    puts "Vagrant Database Cleaning finished"
  end
end
