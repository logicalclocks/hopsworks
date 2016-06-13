# CI::Reporter::RSpec

Connects [RSpec][rspec] to [CI::Reporter][ci], and then to your CI
system.

[![Gem Version](https://badge.fury.io/rb/ci_reporter_rspec.svg)](http://badge.fury.io/rb/ci_reporter_rspec)
[![Build Status](https://travis-ci.org/ci-reporter/ci_reporter_rspec.svg?branch=master)](https://travis-ci.org/ci-reporter/ci_reporter_rspec)
[![Dependency Status](https://gemnasium.com/ci-reporter/ci_reporter_rspec.svg)](https://gemnasium.com/ci-reporter/ci_reporter_rspec)
[![Code Climate](https://codeclimate.com/github/ci-reporter/ci_reporter_rspec.png)](https://codeclimate.com/github/ci-reporter/ci_reporter_rspec)

[rspec]: https://www.relishapp.com/rspec
[ci]: https://github.com/ci-reporter/ci_reporter

## Supported versions

The latest release of RSpec 2.14 and 2.99 are supported. RSpec 3 is
not yet supported.

## Installation

Add this line to your application's Gemfile:

```ruby
gem 'ci_reporter_rspec'
```

And then install it:

```
$ bundle
```

## Usage

Require the reporter in your Rakefile, and ensure that
`ci:setup:rspec` is a dependency of your RSpec task:

```ruby
require 'ci/reporter/rake/rspec'

# ...
# Rake code that creates a task called `:rspec`
# ...

task :rspec => 'ci:setup:rspec'
```

### Advanced usage

Refer to the shared [documentation][ci] for details on setting up
CI::Reporter.

### `rspec-rails`

If you use the [rspec-rails][rspec-rails] gem, you can follow the
example above and use the predefined Rake target `:spec`.

[rspec-rails]: https://www.relishapp.com/rspec/rspec-rails/docs

## Formatters

CI::Reporter has separate Rake tasks for each built-in RSpec
formatter. Depending upon which formatter you would like, call the
corresponding task.

| Formatter          | Task               |
|--------------------|--------------------|
| Progress (default) | ci:setup:rspec     |
| Base               | ci:setup:rspecbase |
| Documentation      | ci:setup:rspecdoc  |

## Contributing

1. Fork it ( https://github.com/ci-reporter/ci_reporter_rspec/fork )
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Add a failing test.
4. Commit your changes (`git commit -am 'Add some feature'`)
5. Ensure tests pass.
6. Push to the branch (`git push origin my-new-feature`)
7. Create a new Pull Request
