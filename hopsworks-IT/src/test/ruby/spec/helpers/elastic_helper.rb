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

  def check_index_not_found(response)
    expect(response.status).to eq(401)
    body = JSON.parse(response.body)
    expect(body["error"]["type"]).to  eq("index_not_found_exception")
  end
end