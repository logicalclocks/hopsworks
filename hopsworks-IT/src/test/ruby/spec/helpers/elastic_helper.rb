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
module ElasticHelper
  def elastic_post(path, body)
    uri = URI.parse("https://#{ENV['ELASTIC_API']}/#{path}")
    request = Net::HTTP::Post.new(uri)
    request.basic_auth("#{ENV['ELASTIC_USER']}", "#{ENV['ELASTIC_PASS']}")
    request.body = body

    req_options = {
        use_ssl: uri.scheme == "https",
        verify_mode: OpenSSL::SSL::VERIFY_NONE,
    }

    Net::HTTP.start(uri.hostname, uri.port, req_options) do |http| http.request(request) end
  end

  def elastic_get(path)
    uri = URI.parse("https://#{ENV['ELASTIC_API']}/#{path}")
    request = Net::HTTP::Get.new(uri)
    request.basic_auth("#{ENV['ELASTIC_USER']}", "#{ENV['ELASTIC_PASS']}")

    req_options = {
        use_ssl: uri.scheme == "https",
        verify_mode: OpenSSL::SSL::VERIFY_NONE,
    }

    Net::HTTP.start(uri.hostname, uri.port, req_options) do |http| http.request(request) end
  end

  def elastic_head(path)
    uri = URI.parse("https://#{ENV['ELASTIC_API']}/#{path}")
    request = Net::HTTP::Head.new(uri)
    request.basic_auth("#{ENV['ELASTIC_USER']}", "#{ENV['ELASTIC_PASS']}")

    req_options = {
        use_ssl: uri.scheme == "https",
        verify_mode: OpenSSL::SSL::VERIFY_NONE,
    }

    Net::HTTP.start(uri.hostname, uri.port, req_options) do |http| http.request(request) end
  end

  def elastic_delete(path)
    uri = URI.parse("https://#{ENV['ELASTIC_API']}/#{path}")
    request = Net::HTTP::Delete.new(uri)
    request.basic_auth("#{ENV['ELASTIC_USER']}", "#{ENV['ELASTIC_PASS']}")

    req_options = {
        use_ssl: uri.scheme == "https",
        verify_mode: OpenSSL::SSL::VERIFY_NONE,
    }

    Net::HTTP.start(uri.hostname, uri.port, req_options) do |http| http.request(request) end
  end

  def elastic_rest
    begin
      Airborne.configure do |config|
        config.base_url = ''
      end
      yield
    ensure
      Airborne.configure do |config|
        config.base_url = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
      end
    end
  end

  #modified variant of expect_status where we print a mode detailed error msg
  #and we also have the ability to check for hopsworks error_code
  def elastic_status_details(response, expected_status, error_type: nil)
    #204 doesn't have a response body - treat differently
    if response.code == resolve_status(204, response.code)
      #print the usual expected/found msg
      expect(response.code).to eq(resolve_status(expected_status, response.code)), "expected rest status:#{expected_status}, found:#{response.code}"
    else
      parsed_body = JSON.parse(response.body) rescue nil
      if parsed_body
        #print the usual expected/found msg but also the full response body for more details
        expect(response.code).to eq(resolve_status(expected_status, response.code)), "expected rest status:#{expected_status}, found:#{response.code} and body:#{parsed_body}"
        #if hopsworks error type - check
        if error_type
          expect(error_type).to eq(parsed_body["error"]["type"]), "expected error type:#{error_type}, found:#{parsed_body}"
        end
      else
        #couldn't pare the body print only the usual expected/found msg
        expect(response.code).to eq(resolve_status(expected_status, response.code)), "found code:#{response.code} and malformed body:#{response.body}"
      end
    end
  end
end