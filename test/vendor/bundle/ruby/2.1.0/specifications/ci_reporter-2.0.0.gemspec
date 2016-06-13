# -*- encoding: utf-8 -*-
# stub: ci_reporter 2.0.0 ruby lib

Gem::Specification.new do |s|
  s.name = "ci_reporter".freeze
  s.version = "2.0.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Nick Sieger".freeze, "Jake Goulding".freeze]
  s.date = "2014-07-24"
  s.description = "CI::Reporter is an add-on to Ruby testing frameworks that allows you to generate XML reports of your test runs. The resulting files can be read by a continuous integration system that understands Ant's JUnit report format.".freeze
  s.email = ["nick@nicksieger.com".freeze, "jake.goulding@gmail.com".freeze]
  s.extra_rdoc_files = ["History.txt".freeze, "LICENSE.txt".freeze, "README.md".freeze]
  s.files = ["History.txt".freeze, "LICENSE.txt".freeze, "README.md".freeze]
  s.homepage = "https://github.com/ci-reporter/ci_reporter".freeze
  s.licenses = ["MIT".freeze]
  s.rubygems_version = "2.6.3".freeze
  s.summary = "Connects Ruby test frameworks to CI systems via JUnit reports.".freeze

  s.installed_by_version = "2.6.3" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<builder>.freeze, [">= 2.1.2"])
      s.add_development_dependency(%q<rake>.freeze, [">= 0"])
      s.add_development_dependency(%q<rdoc>.freeze, ["~> 4.0"])
      s.add_development_dependency(%q<rspec>.freeze, ["~> 3.0"])
    else
      s.add_dependency(%q<builder>.freeze, [">= 2.1.2"])
      s.add_dependency(%q<rake>.freeze, [">= 0"])
      s.add_dependency(%q<rdoc>.freeze, ["~> 4.0"])
      s.add_dependency(%q<rspec>.freeze, ["~> 3.0"])
    end
  else
    s.add_dependency(%q<builder>.freeze, [">= 2.1.2"])
    s.add_dependency(%q<rake>.freeze, [">= 0"])
    s.add_dependency(%q<rdoc>.freeze, ["~> 4.0"])
    s.add_dependency(%q<rspec>.freeze, ["~> 3.0"])
  end
end
