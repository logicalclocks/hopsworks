# -*- encoding: utf-8 -*-
# stub: rubocop 0.36.0 ruby lib

Gem::Specification.new do |s|
  s.name = "rubocop".freeze
  s.version = "0.36.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Bozhidar Batsov".freeze, "Jonas Arvidsson".freeze, "Yuji Nakayama".freeze]
  s.date = "2016-01-14"
  s.description = "    Automatic Ruby code style checking tool.\n    Aims to enforce the community-driven Ruby Style Guide.\n".freeze
  s.email = "rubocop@googlegroups.com".freeze
  s.executables = ["rubocop".freeze]
  s.extra_rdoc_files = ["LICENSE.txt".freeze, "README.md".freeze]
  s.files = ["LICENSE.txt".freeze, "README.md".freeze, "bin/rubocop".freeze]
  s.homepage = "http://github.com/bbatsov/rubocop".freeze
  s.licenses = ["MIT".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3".freeze)
  s.rubygems_version = "2.6.3".freeze
  s.summary = "Automatic Ruby code style checking tool.".freeze

  s.installed_by_version = "2.6.3" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rainbow>.freeze, ["< 3.0", ">= 1.99.1"])
      s.add_runtime_dependency(%q<parser>.freeze, ["< 3.0", ">= 2.3.0.0"])
      s.add_runtime_dependency(%q<powerpack>.freeze, ["~> 0.1"])
      s.add_runtime_dependency(%q<ruby-progressbar>.freeze, ["~> 1.7"])
      s.add_development_dependency(%q<bundler>.freeze, ["~> 1.3"])
    else
      s.add_dependency(%q<rainbow>.freeze, ["< 3.0", ">= 1.99.1"])
      s.add_dependency(%q<parser>.freeze, ["< 3.0", ">= 2.3.0.0"])
      s.add_dependency(%q<powerpack>.freeze, ["~> 0.1"])
      s.add_dependency(%q<ruby-progressbar>.freeze, ["~> 1.7"])
      s.add_dependency(%q<bundler>.freeze, ["~> 1.3"])
    end
  else
    s.add_dependency(%q<rainbow>.freeze, ["< 3.0", ">= 1.99.1"])
    s.add_dependency(%q<parser>.freeze, ["< 3.0", ">= 2.3.0.0"])
    s.add_dependency(%q<powerpack>.freeze, ["~> 0.1"])
    s.add_dependency(%q<ruby-progressbar>.freeze, ["~> 1.7"])
    s.add_dependency(%q<bundler>.freeze, ["~> 1.3"])
  end
end
