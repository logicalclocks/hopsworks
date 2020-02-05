=begin
 Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

 This file is part of Hopsworks
 Copyright (C) 2018, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.

 Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

 Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved

 Permission is hereby granted, free of charge, to any person obtaining a copy of this
 software and associated documentation files (the "Software"), to deal in the Software
 without restriction, including without limitation the rights to use, copy, modify, merge,
 publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or
 substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
=end

require 'hops-airborne'
require 'byebug'
require 'active_record'

#require 'launchy'

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
  config.include Helper
  config.include SessionHelper
  config.include ProjectHelper
  config.include FactoryHelper
  config.include DatasetHelper
  config.include VariablesHelper
  config.include CondaHelper
  config.include CaHelper
  config.include HostsHelper
  config.include KafkaHelper
  config.include KafkaAclHelper
  config.include ServingHelper
  config.include HopsFSHelper
  config.include JobHelper
  config.include ExecutionHelper
  config.include FeaturestoreHelper
  config.include AgentHelper
  config.include PythonHelper
  config.include QuotaHelper
  config.include JupyterHelper
  config.include UsersHelper
  config.include ApiKeyHelper
  config.include AdminUsersHelper
  config.include SchemaHelper
  config.include ProvStateHelper
  config.include ExperimentHelper
  config.include ModelHelper
  config.include ElasticHelper
  config.include AuditHelper
  # uncomment next line if you need to clean hdfs and hopsworks db before test.
  # config.before(:suite) { clean_test_data }
  config.after(:suite) {
    # If we are not using Jenkins, then clean the data
    if ARGV.grep(/spec\.rb/).empty? && (!ENV['JENKINS'] || ENV['JENKINS'] == "false")
      clean_test_data
    end

#    if ENV['LAUNCH_BROWSER'] && ENV['LAUNCH_BROWSER']=="true"
#       Launchy.open("#{ENV['PROJECT_DIR']}#{ENV['RSPEC_REPORT']}")
#    end
  }
  if ENV['SKIP_VM_TEST'] == "true" # Skip tests tagged with vm: true
    config.filter_run_excluding vm: true
  end
  config.filter_run focus: true
  config.run_all_when_everything_filtered = true
end

Airborne.configure do |config|
  config.base_url = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
  config.timeout = 120
end

def clean_test_data
  puts "Cleaning test data ..."
  require 'net/ssh'
  if ENV['RSPEC_SSH'] && ENV['RSPEC_SSH']=="true"
    Net::SSH.start("#{ENV['RSPEC_SSH_HOST']}", "#{ENV['RSPEC_SSH_USER']}") do |ssh|
      puts "Remote HDFS Clean-up starting..."
      ssh.exec!("cd #{ENV['RSPEC_SSH_USER_DIR']}; vagrant ssh -c 'sudo -u #{ENV['RSPEC_VAGRANT_HDFS_USER']} -H sh -c \" /srv/hops/hadoop/bin/hadoop fs -rm -f -R -skipTrash /Projects \" '")
      puts "Remote HDFS Clean-up finished."

      puts "Database Clean-up starting..."
      sh.exec!("cd #{ENV['RSPEC_SSH_USER_DIR']}; vagrant ssh -c  'sudo -u #{ENV['RSPEC_VAGRANT_MYSQL_USER']} -H sh -c \" /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh -e \\\" DROP DATABASE IF EXISTS hopsworks \\\" \" ' ")
      sh.exec!("cd #{ENV['RSPEC_SSH_USER_DIR']}; vagrant ssh -c  'sudo -u #{ENV['RSPEC_VAGRANT_MYSQL_USER']} -H sh -c \" /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh -e \\\" CREATE DATABASE IF NOT EXISTS hopsworks CHARACTER SET latin1 \\\" \" ' ")
      sh.exec!("cd #{ENV['RSPEC_SSH_USER_DIR']}; vagrant ssh -c  'sudo -u root -H sh -c \" /srv/hops/domains/domain1/flyway/flyway migrate \" ' ")
      sh.exec!("cd #{ENV['RSPEC_SSH_USER_DIR']}; vagrant ssh -c  'sudo -u root -H sh -c \" cat /srv/hops/domains/domain1/flyway/dml/*.sql | /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks \" ' ")
      sh.exec("cd #{ENV['RSPEC_SSH_USER_DIR']}; vagrant ssh -c  'sudo -u root -H sh -c \" /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks -e \\\" UPDATE hosts SET registered=1; \\\" \" ' ")
      puts "Database Clean-up finished."
    end
  else
    puts "Vagrant HDFS Clean-up starting..."
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c '/srv/hops/hadoop/bin/hadoop fs -rm -f -R -skipTrash /Projects ' ")
    puts "Vagrant HDFS Clean-up finished."

    puts "Database Clean-up starting..."
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo -u #{ENV['RSPEC_VAGRANT_MYSQL_USER']} -H sh -c \" /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh -e \\\" DROP DATABASE IF EXISTS hopsworks \\\" \" ' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo -u #{ENV['RSPEC_VAGRANT_MYSQL_USER']} -H sh -c \" /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh -e \\\" CREATE DATABASE IF NOT EXISTS hopsworks CHARACTER SET latin1 \\\" \" ' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo -u root -H sh -c \" /srv/hops/domains/domain1/flyway/flyway migrate \" ' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo -u root -H sh -c \" cat /srv/hops/domains/domain1/flyway/dml/*.sql | /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks \" ' ")
    system("cd #{ENV['RSPEC_USER_DIR']}; vagrant ssh -c 'sudo -u root -H sh -c \" /srv/hops/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks -e \\\" UPDATE hosts SET registered=1 WHERE id=1; \\\" \" ' ")
    puts "Database Clean-up finished."
  end
end
