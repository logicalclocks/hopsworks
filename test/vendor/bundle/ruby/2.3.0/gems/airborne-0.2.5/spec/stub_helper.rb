require 'webmock/rspec'

module StubHelper
  def initialize(*args)
    @base_url = 'http://www.example.com/'
  end

  def mock_get(url, response_headers = {}, status = 200)
    stub_request(:get, @base_url + url).to_return(headers: response_headers, body: get_json_response_file(url), status: status)
  end

  def mock_post(url, options = {}, status = 200)
    stub_request(:post, @base_url + url).with(body: options[:request_body] || {})
      .to_return(headers: options[:response_headers] || {}, body: get_json_response_file(url), status: status)
  end

  def mock_put(url, options = {}, status = 200)
    stub_request(:put, @base_url + url).with(body: options[:request_body] || {})
      .to_return(headers: options[:response_headers] || {}, body: get_json_response_file(url), status: status)
  end

  def mock_patch(url, options = {}, status = 200)
    stub_request(:patch, @base_url + url).with(body: options[:request_body] || {})
      .to_return(headers: options[:response_headers] || {}, body: get_json_response_file(url), status: status)
  end

  def mock_delete(url, options = {}, status = 200)
    stub_request(:delete, @base_url + url).with(body: options[:request_body] || {})
    .to_return(headers: options[:response_headers] || {}, body: get_json_response_file(url), status: status)
  end

  def mock_head(url, response_headers = {}, status = 200)
    stub_request(:head, @base_url + url).to_return(headers: response_headers, body: nil, status: status)
  end

  def mock_options(url, response_headers = {}, status = 200)
    stub_request(:options, @base_url + url).to_return(headers: response_headers, body: nil, status: status)
  end

  private

  def get_json_response_file(name)
    IO.read("spec/test_responses/#{name}.json")
  end
end
