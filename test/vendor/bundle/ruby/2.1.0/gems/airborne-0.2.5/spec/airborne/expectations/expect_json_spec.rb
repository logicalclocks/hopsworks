require 'spec_helper'

describe 'expect_json' do
  it 'should ensure correct json values' do
    mock_get('simple_get')
    get '/simple_get'
    expect_json(name: 'Alex', age: 32)
  end

  it 'should allow array response' do
    mock_get('array_response')
    get '/array_response'
    expect_json([{ name: 'Seth' }])
  end

  it 'should fail when incorrect json is tested' do
    mock_get('simple_get')
    get '/simple_get'
    expect { expect_json(bad: 'data') }.to raise_error(ExpectationNotMetError)
  end

  it 'should allow full object graph' do
    mock_get('simple_path_get')
    get '/simple_path_get'
    expect_json(name: 'Alex', address: { street: 'Area 51', city: 'Roswell', state: 'NM' })
  end
end
