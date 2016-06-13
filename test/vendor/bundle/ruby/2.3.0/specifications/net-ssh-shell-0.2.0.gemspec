# -*- encoding: utf-8 -*-
# stub: net-ssh-shell 0.2.0 ruby lib

Gem::Specification.new do |s|
  s.name = "net-ssh-shell"
  s.version = "0.2.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.3.6") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Jamis Buck"]
  s.date = "2011-06-13"
  s.description = "A simple library to aid with stateful shell interactions"
  s.email = ["jamis@jamisbuck.org"]
  s.homepage = "http://github.com/mitchellh/net-ssh-shell"
  s.rubyforge_project = "net-ssh-shell"
  s.rubygems_version = "2.5.1"
  s.summary = "A simple library to aid with stateful shell interactions"

  s.installed_by_version = "2.5.1" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<net-ssh>, ["~> 2.1.0"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<net-ssh>, ["~> 2.1.0"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<net-ssh>, ["~> 2.1.0"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
