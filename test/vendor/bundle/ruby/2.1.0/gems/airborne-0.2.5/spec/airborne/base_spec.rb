require 'spec_helper'

describe 'base spec' do
  it 'when request is made response should be set' do
    mock_get('simple_get')
    get '/simple_get'
    expect(response).to_not be(nil)
  end

  it 'when request is made headers should be set' do
    mock_get('simple_get')
    get '/simple_get'
    expect(headers).to_not be(nil)
  end

  it 'should throw an InvalidJsonError when accessing json_body on invalid json' do
    mock_get('invalid_json')
    get '/invalid_json'
    expect(body).to eq('1234')
    expect { json_body }.to raise_error(InvalidJsonError)
  end

  it 'when request is made headers should be hash with indifferent access' do
    mock_get('simple_get', 'Content-Type' => 'application/json')
    get '/simple_get'
    expect(headers).to be_kind_of(Hash)
    expect(headers[:content_type]).to eq('application/json')
    expect(headers['content_type']).to eq('application/json')
  end

  it 'when request is made body should be set' do
    mock_get('simple_get')
    get '/simple_get'
    expect(body).to_not be(nil)
  end

  it 'when request is made json body should be symbolized hash' do
    mock_get('simple_get')
    get '/simple_get'
    expect(json_body).to be_kind_of(Hash)
    expect(json_body.first[0]).to be_kind_of(Symbol)
  end

  it 'should handle a 500 error on get' do
    mock_get('simple_get', {}, [500, 'Internal Server Error'])
    get '/simple_get'
    expect(json_body).to_not be(nil)
  end

  it 'should handle a 500 error on post' do
    mock_post('simple_post', {}, [500, 'Internal Server Error'])
    post '/simple_post', {}
    expect(json_body).to_not be(nil)
  end
end
