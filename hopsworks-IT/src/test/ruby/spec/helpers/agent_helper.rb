=begin
 This file is part of Hopsworks
 Copyright (C) 2019, Logical Clocks AB. All rights reserved
 
 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.
 
 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.
 
 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

require 'net/ssh'

module AgentHelper

    @@SYSTEMCTL_IS_ACTIVE = "systemctl show unit.service -p ActiveState"

    @@KAGENT_START = "sudo systemctl start kagent"
    @@KAGENT_STOP = "sudo systemctl stop kagent"

    @@SPARKHISTORYSERVER_START = "systemctl start sparkhistoryserver"
    @@SPARKHISTORYSERVER_STOP = "systemctl stop sparkhistoryserver"

    @@username = ENV.fetch('REMOTE_SSH_USER', 'vagrant')
    @@port = ENV.fetch('REMOTE_SSH_PORT', '22')
    @@password = ENV.fetch('REMOTE_SSH_PASSWORD', 'vagrant')

    @@hosts = %w[hopsworks0 hopsworks1 hopsworks2]
    @@all_services = %w[
        ndb_mgmd
        alertmanager
        node_exporter
        ndbmtd
        mysqld
        mysqld_exporter
        glassfish-domain1
        kagent
        consul
        prometheus
        grafana
        pushgateway
        opensearch
        elastic_exporter
        namenode
        zookeeper
        kubelet
        docker
        datanode
        kafka
        airflow-webserver
        airflow-scheduler
        epipe
        historyserver
        resourcemanager
        logstash
        opensearch-dashboards
        hivemetastore
        hiveserver2
        onlinefs
        livy
        flinkhistoryserver
        nodemanager
        sparkhistoryserver
        filebeat-spark
        filebeat-jupyter
        filebeat-service
        filebeat-tf-serving
        filebeat-sklearn-serving
    ]

    def kagent_start(hostname)
        execute_remotely hostname, @@KAGENT_START
    end

    def kagent_stop(hostname)
        execute_remotely hostname, @@KAGENT_STOP
    end

    def sparkhistoryserver_start(hostname)
        execute_remotely hostname, @@SPARKHISTORYSERVER_START
    end

    def sparkhistoryserver_stop(hostname)
        execute_remotely hostname, @@SPARKHISTORYSERVER_STOP
    end

    def is_service_running(service, hostname)
      output = execute_remotely hostname, @@SYSTEMCTL_IS_ACTIVE.gsub("unit", service)
      output.strip.eql? "ActiveState=active"
    end

    def is_service_dead(service, hostname)
        output = execute_remotely hostname, @@SYSTEMCTL_IS_ACTIVE.gsub("unit", service)
        output.strip.eql? "ActiveState=failed"
    end

    def is_service_dead_local(service)
        output = %x[#{@@SYSTEMCTL_IS_ACTIVE.gsub("unit", service)}]
        output.strip.eql? "ActiveState=failed"
    end

    def execute_remotely(hostname, command)
        Net::SSH.start(hostname, @@username, :port => @@port,
            :password => @@password, :verify_host_key => :never) do |ssh|
            output = ssh.exec!(command)
        end
    end

    def test_all_services
        dead_services = []
        @@all_services.each { |service|
            if "#{ENV['OS']}" == "ubuntu"
                if is_service_dead_local service
                    dead_services.push("#{service} is dead on hopsworks0")
                    %x[sudo systemctl start #{service}]
                end
            else
                @@hosts.each { |host|
                    if is_service_dead service, host
                        dead_services.push("#{service} is dead on #{host}")
                        execute_remotely host, "sudo systemctl start #{service}"
                    end
                }
            end
        }
        dead_services
    end

    def wait_for_services(wait_time: 600)
        dead_services = test_all_services
        if dead_services.empty?
            return { "success" => true, "deadServices" => 0 }
        end
        wait_for_me_time(wait_time, 60) do
            dead_services = test_all_services
            if dead_services.empty?
                { "success" => true, "deadServices" => 0 }
            else
                { "msg" => "Dead services - #{dead_services}", "success" => false, "deadServices" => dead_services.length }
            end
        end
        dead_services = test_all_services
        unless dead_services.empty?
            pp "WARNING - Dead services - #{dead_services}"
        end
    end
end
