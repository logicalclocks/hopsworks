require 'spec_helper'

describe 'client requester' do
  before do
    allow(RestClient).to receive(:send)
    RSpec::Mocks.space.proxy_for(self).remove_stub_if_present(:get)
  end

  after do
    allow(RestClient).to receive(:send).and_call_original
    Airborne.configure { |config| config.headers =  {} }
  end

  it 'should set :content_type to :json by default' do
    get '/foo'

    expect(RestClient).to have_received(:send)
                            .with(:get, 'http://www.example.com/foo', { content_type: :json })
  end

  it 'should override headers with option[:headers]' do
    get '/foo', { content_type: 'application/x-www-form-urlencoded' }

    expect(RestClient).to have_received(:send)
                            .with(:get, 'http://www.example.com/foo', { content_type: 'application/x-www-form-urlencoded' })
  end

  it 'should override headers with airborne config headers' do
    Airborne.configure { |config| config.headers = { content_type: 'text/plain' } }

    get '/foo'

    expect(RestClient).to have_received(:send)
                            .with(:get, 'http://www.example.com/foo', { content_type: 'text/plain' })
  end
end
