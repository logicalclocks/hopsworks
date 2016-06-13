require 'spec_helper'

describe 'expect_status' do
  it 'should verify correct status code' do
    mock_get('simple_get')
    get '/simple_get'
    expect_status 200
  end

  it 'should fail when incorrect status code is returned' do
    mock_get('simple_get')
    get '/simple_get'
    expect { expect_status 123 }.to raise_error(ExpectationNotMetError)
  end

  it 'should translate symbol codes to whatever is appropriate for the request' do
    mock_get('simple_get')
    get '/simple_get'
    expect_status :ok
    expect_status 200
    expect_status '200'
  end
end
