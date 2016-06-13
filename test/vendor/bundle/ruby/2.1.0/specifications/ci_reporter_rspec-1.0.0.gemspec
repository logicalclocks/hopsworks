# -*- encoding: utf-8 -*-
# stub: ci_reporter_rspec 1.0.0 ruby lib

Gem::Specification.new do |s|
  s.name = "ci_reporter_rspec".freeze
  s.version = "1.0.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Nick Sieger".freeze, "Jake Goulding".freeze]
  s.date = "2014-07-24"
  s.email = ["nick@nicksieger.com".freeze, "jake.goulding@gmail.com".freeze]
  s.homepage = "https://github.com/ci-reporter/ci_reporter_rspec".freeze
  s.licenses = ["MIT".freeze]
  s.rubygems_version = "2.6.3".freeze
  s.summary = "Connects CI::Reporter to RSpec".freeze

  s.installed_by_version = "2.6.3" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rspec>.freeze, ["< 4", ">= 2.14"])
      s.add_runtime_dependency(%q<ci_reporter>.freeze, ["~> 2.0"])
      s.add_development_dependency(%q<bundler>.freeze, ["~> 1.6"])
      s.add_development_dependency(%q<rake>.freeze, [">= 0"])
      s.add_development_dependency(%q<ci_reporter_test_utils>.freeze, [">= 0"])
      s.add_development_dependency(%q<rspec-collection_matchers>.freeze, [">= 0"])
    else
      s.add_dependency(%q<rspec>.freeze, ["< 4", ">= 2.14"])
      s.add_dependency(%q<ci_reporter>.freeze, ["~> 2.0"])
      s.add_dependency(%q<bundler>.freeze, ["~> 1.6"])
      s.add_dependency(%q<rake>.freeze, [">= 0"])
      s.add_dependency(%q<ci_reporter_test_utils>.freeze, [">= 0"])
      s.add_dependency(%q<rspec-collection_matchers>.freeze, [">= 0"])
    end
  else
    s.add_dependency(%q<rspec>.freeze, ["< 4", ">= 2.14"])
    s.add_dependency(%q<ci_reporter>.freeze, ["~> 2.0"])
    s.add_dependency(%q<bundler>.freeze, ["~> 1.6"])
    s.add_dependency(%q<rake>.freeze, [">= 0"])
    s.add_dependency(%q<ci_reporter_test_utils>.freeze, [">= 0"])
    s.add_dependency(%q<rspec-collection_matchers>.freeze, [">= 0"])
  end
end
