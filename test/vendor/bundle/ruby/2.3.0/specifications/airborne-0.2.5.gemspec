# -*- encoding: utf-8 -*-
# stub: airborne 0.2.5 ruby lib

Gem::Specification.new do |s|
  s.name = "airborne"
  s.version = "0.2.5"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Alex Friedman", "Seth Pollack"]
  s.date = "2016-04-12"
  s.email = ["a.friedman07@gmail.com", "teampollack@gmail.com"]
  s.licenses = ["MIT"]
  s.rubygems_version = "2.5.1"
  s.summary = "RSpec driven API testing framework"

  s.installed_by_version = "2.5.1" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rspec>, [">= 3.1.0", "~> 3.1"])
      s.add_runtime_dependency(%q<rest-client>, [">= 1.7.3", "~> 1.7"])
      s.add_runtime_dependency(%q<rack-test>, [">= 0.6.2", "~> 0.6"])
      s.add_runtime_dependency(%q<activesupport>, [">= 3.0.0"])
      s.add_development_dependency(%q<webmock>, ["~> 0"])
    else
      s.add_dependency(%q<rspec>, [">= 3.1.0", "~> 3.1"])
      s.add_dependency(%q<rest-client>, [">= 1.7.3", "~> 1.7"])
      s.add_dependency(%q<rack-test>, [">= 0.6.2", "~> 0.6"])
      s.add_dependency(%q<activesupport>, [">= 3.0.0"])
      s.add_dependency(%q<webmock>, ["~> 0"])
    end
  else
    s.add_dependency(%q<rspec>, [">= 3.1.0", "~> 3.1"])
    s.add_dependency(%q<rest-client>, [">= 1.7.3", "~> 1.7"])
    s.add_dependency(%q<rack-test>, [">= 0.6.2", "~> 0.6"])
    s.add_dependency(%q<activesupport>, [">= 3.0.0"])
    s.add_dependency(%q<webmock>, ["~> 0"])
  end
end
