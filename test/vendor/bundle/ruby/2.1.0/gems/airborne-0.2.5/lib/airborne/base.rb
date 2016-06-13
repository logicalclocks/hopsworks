require 'json'
require 'active_support'
require 'active_support/core_ext/hash/indifferent_access'

module Airborne
  class InvalidJsonError < StandardError; end

  include RequestExpectations

  attr_reader :response, :headers, :body

  def self.configure
    RSpec.configure do |config|
      yield config
    end
  end

  def self.included(base)
    if !Airborne.configuration.requester_module.nil?
      base.send(:include, Airborne.configuration.requester_module)
    elsif !Airborne.configuration.rack_app.nil?
      base.send(:include, RackTestRequester)
    else
      base.send(:include, RestClientRequester)
    end
  end

  def self.configuration
    RSpec.configuration
  end

  def get(url, headers = nil)
    @response = make_request(:get, url, headers: headers)
  end

  def post(url, post_body = nil, headers = nil)
    @response = make_request(:post, url, body: post_body, headers: headers)
  end

  def patch(url, patch_body = nil, headers = nil)
    @response = make_request(:patch, url, body: patch_body, headers: headers)
  end

  def put(url, put_body = nil, headers = nil)
    @response = make_request(:put, url, body: put_body, headers: headers)
  end

  def delete(url, delete_body = nil, headers = nil)
    @response = make_request(:delete, url, body: delete_body, headers: headers)
  end

  def head(url, headers = nil)
    @response = make_request(:head, url, headers: headers)
  end

  def options(url, headers = nil)
    @response = make_request(:options, url, headers: headers)
  end

  def response
    @response
  end

  def headers
    HashWithIndifferentAccess.new(response.headers)
  end

  def body
    response.body
  end

  def json_body
    JSON.parse(response.body, symbolize_names: true) rescue fail InvalidJsonError, 'Api request returned invalid json'
  end

  private

  def get_url(url)
    base = Airborne.configuration.base_url || ''
    base + url
  end
end
