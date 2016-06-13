require 'spec_helper'

describe 'patch' do
  it 'should allow testing on patch requests' do
    mock_patch('simple_patch')
    patch '/simple_patch', {}
    expect_json_types(status: :string, someNumber: :int)
  end
end
