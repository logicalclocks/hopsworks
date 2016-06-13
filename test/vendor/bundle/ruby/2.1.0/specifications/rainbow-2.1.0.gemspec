# -*- encoding: utf-8 -*-
# stub: rainbow 2.1.0 ruby lib

Gem::Specification.new do |s|
  s.name = "rainbow".freeze
  s.version = "2.1.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Marcin Kulik".freeze]
  s.date = "2016-01-24"
  s.description = "Colorize printed text on ANSI terminals".freeze
  s.email = ["m@ku1ik.com".freeze]
  s.homepage = "https://github.com/sickill/rainbow".freeze
  s.licenses = ["MIT".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.2".freeze)
  s.rubygems_version = "2.6.3".freeze
  s.summary = "Colorize printed text on ANSI terminals".freeze

  s.installed_by_version = "2.6.3" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>.freeze, ["~> 1.3"])
    else
      s.add_dependency(%q<bundler>.freeze, ["~> 1.3"])
    end
  else
    s.add_dependency(%q<bundler>.freeze, ["~> 1.3"])
  end
end
