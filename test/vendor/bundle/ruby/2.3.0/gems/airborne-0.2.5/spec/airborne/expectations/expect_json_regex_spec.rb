require 'spec_helper'

describe 'expect_json regex' do
  it 'should test against regex' do
    mock_get('simple_get')
    get '/simple_get'
    expect_json(name: regex('^A'))
  end

  it 'should raise an error if regex does not match' do
    mock_get('simple_get')
    get '/simple_get'
    expect { expect_json(name: regex('^B')) }.to raise_error(ExpectationNotMetError)
  end

  it 'should allow regex(Regexp) to be tested against a path' do
    mock_get('simple_nested_path')
    get '/simple_nested_path'
    expect_json('address.city', regex('^R'))
  end

  it 'should allow testing regex against numbers directly' do
    mock_get('simple_nested_path')
    get '/simple_nested_path'
    expect_json('address.coordinates.lattitude', regex('^3'))
  end

  it 'should allow testing regex against numbers in the hash' do
    mock_get('simple_nested_path')
    get '/simple_nested_path'
    expect_json('address.coordinates', lattitude: regex('^3'))
  end
end
