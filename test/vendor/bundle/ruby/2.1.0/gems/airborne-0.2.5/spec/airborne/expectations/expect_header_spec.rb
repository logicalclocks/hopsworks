require 'spec_helper'

describe 'expect header' do
  it 'should find exact match for header content' do
    mock_get('simple_get', 'Content-Type' => 'application/json')
    get '/simple_get'
    expect_header(:content_type, 'application/json')
  end

  it 'should find exact match for header content' do
    mock_get('simple_get', 'Content-Type' => 'json')
    get '/simple_get'
    expect { expect_header(:content_type, 'application/json') }.to raise_error(ExpectationNotMetError)
  end

  it 'should ensure correct headers are present' do
    mock_get('simple_get', 'Content-Type' => 'application/json')
    get '/simple_get'
    expect { expect_header(:foo, 'bar') }.to raise_error(ExpectationNotMetError)
  end
end
