# -*- encoding: utf-8 -*-
# stub: ci_reporter 2.0.0 ruby lib

Gem::Specification.new do |s|
  s.name = "ci_reporter"
  s.version = "2.0.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Nick Sieger", "Jake Goulding"]
  s.date = "2014-07-24"
  s.description = "CI::Reporter is an add-on to Ruby testing frameworks that allows you to generate XML reports of your test runs. The resulting files can be read by a continuous integration system that understands Ant's JUnit report format."
  s.email = ["nick@nicksieger.com", "jake.goulding@gmail.com"]
  s.extra_rdoc_files = ["History.txt", "LICENSE.txt", "README.md"]
  s.files = ["History.txt", "LICENSE.txt", "README.md"]
  s.homepage = "https://github.com/ci-reporter/ci_reporter"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.5.1"
  s.summary = "Connects Ruby test frameworks to CI systems via JUnit reports."

  s.installed_by_version = "2.5.1" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<builder>, [">= 2.1.2"])
      s.add_development_dependency(%q<rake>, [">= 0"])
      s.add_development_dependency(%q<rdoc>, ["~> 4.0"])
      s.add_development_dependency(%q<rspec>, ["~> 3.0"])
    else
      s.add_dependency(%q<builder>, [">= 2.1.2"])
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<rdoc>, ["~> 4.0"])
      s.add_dependency(%q<rspec>, ["~> 3.0"])
    end
  else
    s.add_dependency(%q<builder>, [">= 2.1.2"])
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<rdoc>, ["~> 4.0"])
    s.add_dependency(%q<rspec>, ["~> 3.0"])
  end
end
