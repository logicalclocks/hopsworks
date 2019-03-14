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
  describe 'Dela' do

    before :all do
      @csr = "-----BEGIN CERTIFICATE REQUEST-----\n" +
          "MIICuTCCAaECAQAwdDELMAkGA1UEBhMCU0UxEjAQBgNVBAgMCVN0b2NraG9sbTEL" +
          "MAkGA1UEBwwCU0UxCjAIBgNVBAsMATExCzAJBgNVBAoMAlNFMQ0wCwYDVQQDDAR0" +
          "ZXN0MRwwGgYJKoZIhvcNAQkBFg1hZ2VudEBob3BzLmlvMIIBIjANBgkqhkiG9w0B" +
          "AQEFAAOCAQ8AMIIBCgKCAQEApc1xai9zyHlc/su4w34Qas67OooqagykHpkk8dBH" +
          "yGcB7BLhxpgqC3odZJ6PoepPugYETcDCCgYMNxoke/TOaTpXwD+wX4Nwl1zMgzVf" +
          "D8aZQ+Ns9Rjf8vF3P+KDL3UCUxmNuX17Vew2jfrEQMap7CC38+Ss4eUCehc0num1" +
          "IbIyH1pv/Qa/7akscjVbfVWFZ5JlahzIbSRByPQtx2lBzMwxn/dydnfol+uu5tEZ" +
          "Uk0Pjr9h8n0ujI+MTfrAgUuYCDCYU+gX1hU9CiWTpghlzxwt3djG/hc/ZBK/50Bm" +
          "ds6db+u/oRoPQV/JW36bBYifaLmKfwuBwJC78YNdRgyaIQIDAQABoAAwDQYJKoZI" +
          "hvcNAQELBQADggEBAB4tuWccPaUHaGAygm+7WoVIPn5dtpqHcvj6rBc3VHRDRHKL" +
          "GVp0gjDYqVdxV54qTm8fBLGz2zB27kR1lVp+ctwqE/HZ9uCJOMcG1P1ZSrMExEF/" +
          "re2SaLDKhtrOYBWS+6ZERv7ndzRjn9Q2BBeyBkmRfLT4TSXVmhUVas2JAD6jrlei" +
          "ai2Nga3rPpSL/2SOG+uuKwv83yC5rnT+6KIAV+MxOTXuz7QXGAxKtts7iPE8ShFf" +
          "DOnceMjdaX/jqJnf/6UdO02/tYqL59XJldeFe2WuflOrNE5pArdAhZiqkSUgm6ox" +
          "V7eQcEU4CJXHJArObgFdbE50nm9cFdW36DPmZAc=\n" +
          "-----END CERTIFICATE REQUEST-----"

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
      post "#{ENV['HOPSWORKS_DELA_TRACKER']}/cluster/certificate", {csr: @csr}
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
      post "#{ENV['HOPSWORKS_DELA_TRACKER']}/cluster/certificate", {csr: @csr}
      expect_status(200)

      # Check that the certificate is on the local fs. this assumes you are running the
      # tests on a proper vm
      check_certificate_exists(@certs_dir + "/intermediate/", "test", @subject)
    end

    it 'should fail to sign twice a certificate for the same cluster' do
      post "#{ENV['HOPSWORKS_DELA_TRACKER']}/cluster/certificate", {csr: @csr}
      expect_status(400)
    end

    it 'should revoke the certificate' do
      delete "#{ENV['HOPSWORKS_DELA_TRACKER']}/cluster/certificate?certId=test"
      expect_status(200)
    end
  end
end
