require 'json'
require 'sinatra'

class SampleApp < Sinatra::Application
  before do
    content_type 'application/json'
  end

  get '/' do
    { foo: 'bar' }.to_json
  end
end

Airborne.configure do |config|
  config.rack_app = SampleApp
end

describe 'rack app' do
  it 'should allow requests against a sinatra app' do
    get '/'
    expect_json_types(foo: :string)
  end

  it 'should ensure correct values from sinatra app' do
    get '/'
    expect { expect_json_types(foo: :int) }.to raise_error(ExpectationNotMetError)
  end

  it 'Should set json_body even when not using the airborne http requests' do
    Response = Struct.new(:body, :headers)
    @response = Response.new({ foo: 'bar' }.to_json)
    expect(json_body).to eq(foo: 'bar')
  end

  it 'Should work with consecutive requests' do
    Response = Struct.new(:body, :headers)
    @response = Response.new({ foo: 'bar' }.to_json)
    expect(json_body).to eq(foo: 'bar')

    @response = Response.new({ foo: 'boo' }.to_json)
    expect(json_body).to eq(foo: 'boo')
  end
end
