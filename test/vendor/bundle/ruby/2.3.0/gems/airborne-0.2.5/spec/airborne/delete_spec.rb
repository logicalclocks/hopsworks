require 'spec_helper'

describe 'delete' do
  it 'should allow testing on delete requests' do
    mock_delete 'simple_delete'
    delete '/simple_delete', {}
    expect_status 200
  end
end
