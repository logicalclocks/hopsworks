require 'ci/reporter/core'

module CI::Reporter
  rspec_version = Gem::Version.new(::RSpec::Core::Version::STRING)
  rspec_3 = Gem::Version.new('3.0.0')
  RSPEC_3_AVAILABLE = rspec_version >= rspec_3

  if RSPEC_3_AVAILABLE
    require 'ci/reporter/rspec3/formatter'
    RSpecFormatter = CI::Reporter::RSpec3::Formatter
  else
    require 'ci/reporter/rspec2/formatter'
    RSpecFormatter = CI::Reporter::RSpec2::Formatter
  end
end
