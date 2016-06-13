require 'spec_helper'

describe 'put' do
  it 'should allow testing on put requests' do
    mock_put('simple_put')
    put '/simple_put', {}
    expect_json_types(status: :string, someNumber: :int)
  end
end
