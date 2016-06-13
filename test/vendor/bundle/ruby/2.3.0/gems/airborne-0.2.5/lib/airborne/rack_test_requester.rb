require 'rack/test'

module Airborne
  module RackTestRequester
    def make_request(method, url, options = {})
      headers = options[:headers] || {}
      base_headers = Airborne.configuration.headers || {}
      headers = base_headers.merge(headers)
      browser = Rack::Test::Session.new(Rack::MockSession.new(Airborne.configuration.rack_app))
      headers.each { |name, value| browser.header(name, value) }
      browser.send(method, url, options[:body] || {}, headers)
      Rack::MockResponse.class_eval do
        alias_method :code, :status
      end
      browser.last_response
    end
  end
end
