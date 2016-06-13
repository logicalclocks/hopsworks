require File.dirname(__FILE__) + "/../../../spec_helper.rb"
require File.dirname(__FILE__) + "/../../../support/rspec2"
require 'ci/reporter/rspec'
require 'ci/reporter/rspec2/failure'

module CI::Reporter::RSpec2
  describe Failure do
    include RSpec2Helpers

    before(:each) do
      skip "RSpec 3 present, skipping RSpec 2 tests" if CI::Reporter::RSPEC_3_AVAILABLE
    end

    before(:each) do
      @formatter = Formatter.new
      @rspec20_example = double('RSpec2.0 Example',
                                :execution_result => {:exception_encountered => StandardError.new('rspec2.0 ftw')},
                                :metadata => {})

      @rspec22_example = rspec2_failing_example('rspec2.2 ftw')
    end

    it 'should handle rspec (< 2.2) execution results' do
      failure = Failure.new(@rspec20_example, @formatter)
      failure.name.should_not be_nil
      failure.message.should == 'rspec2.0 ftw'
      failure.location.should_not be_nil
    end
    it 'should handle rspec (>= 2.2) execution results' do
      failure = Failure.new(@rspec22_example, @formatter)
      failure.name.should_not be_nil
      failure.message.should == 'rspec2.2 ftw'
      failure.location.should_not be_nil
    end
  end
end
