# coding: utf-8
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require 'ci/reporter/version'

Gem::Specification.new do |spec|
  spec.name          = "ci_reporter"
  spec.version       = CI::Reporter::VERSION
  spec.authors       = ["Nick Sieger", "Jake Goulding"]
  spec.email         = ["nick@nicksieger.com", "jake.goulding@gmail.com"]
  spec.homepage      = "https://github.com/ci-reporter/ci_reporter"
  spec.license       = "MIT"

  spec.summary       = %q{Connects Ruby test frameworks to CI systems via JUnit reports.}
  spec.description   = %q{CI::Reporter is an add-on to Ruby testing frameworks that allows you to generate XML reports of your test runs. The resulting files can be read by a continuous integration system that understands Ant's JUnit report format.}

  spec.files            = `git ls-files -z`.split("\x0")
  spec.executables      = spec.files.grep(%r{^bin/}) { |f| File.basename(f) }
  spec.test_files       = spec.files.grep(%r{^(test|spec|features|acceptance)/})
  spec.require_paths    = ["lib"]
  spec.extra_rdoc_files = ["History.txt", "LICENSE.txt", "README.md"]

  spec.add_dependency "builder", ">= 2.1.2"

  spec.add_development_dependency "rake"
  spec.add_development_dependency "rdoc", "~> 4.0"
  spec.add_development_dependency "rspec", "~> 3.0"
end
