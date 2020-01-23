=begin
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
=end

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects}
  describe 'Dela' do

    before :all do
      @certs_dir = Variables.find_by(id: "certs_dir").value
      @subject = "/C=SE/ST=Stockholm/L=SE/O=SE/OU=1/CN=test/emailAddress=agent@hops.io"

      with_cluster_agent_session

      # Remove entries from previous executions
      ClusterCert.destroy_all
    end

    it 'should fail to sign the certificate with empty csr' do
      post "#{ENV['HOPSWORKS_DELA_TRACKER']}/cluster/certificate", {}
      expect_status(422)
    end

    it 'should fail to sign the certificate with no db entry in the db' do
      post "#{ENV['HOPSWORKS_DELA_TRACKER']}/cluster/certificate", {csr: generate_csr(@subject)}
      expect_status(400)
    end

    it 'should sign the dela certificate with entry in the db', vm: true do
      # Add entry in the database
      #   `id` int(11) NOT NULL AUTO_INCREMENT,
      #   `agent_id` int(11) NOT NULL,
      #   `common_name` varchar(64) COLLATE latin1_general_cs NOT NULL,
      #   `organization_name` varchar(64) COLLATE latin1_general_cs NOT NULL,
      #   `organizational_unit_name` varchar(64) COLLATE latin1_general_cs NOT NULL,
      #   `serial_number` varchar(45) COLLATE latin1_general_cs DEFAULT NULL,
      #   `registration_status` varchar(45) COLLATE latin1_general_cs NOT NULL,
      #   `validation_key` varchar(128) COLLATE latin1_general_cs DEFAULT NULL,
      #   `validation_key_date` timestamp NULL DEFAULT NULL,
      #   `registration_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
      #
      # "/C=SE/ST=Stockholm/L=SE/OU=1/O=SE/CN=test/emailAddress=agent@hops.io"
      ClusterCert.create(agent_id: 10001,
                         common_name: "test",
                         organization_name: "SE",
                         organizational_unit_name: "1",
                         registration_status: "Registered")
      post "#{ENV['HOPSWORKS_DELA_TRACKER']}/cluster/certificate", {csr: generate_csr(@subject)}
      expect_status(200)

      # Check that the certificate is on the local fs. this assumes you are running the
      # tests on a proper vm
      check_certificate_exists(@certs_dir + "/intermediate/", "test", @subject)
    end

    it 'should fail to sign twice a certificate for the same cluster' do
      post "#{ENV['HOPSWORKS_DELA_TRACKER']}/cluster/certificate", {csr: generate_csr(@subject)}
      expect_status(400)
    end

    it 'should revoke the certificate' do
      delete "#{ENV['HOPSWORKS_DELA_TRACKER']}/cluster/certificate?certId=test"
      expect_status(200)
    end
  end
end
