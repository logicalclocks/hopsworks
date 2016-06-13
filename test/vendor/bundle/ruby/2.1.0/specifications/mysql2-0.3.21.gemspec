# -*- encoding: utf-8 -*-
# stub: mysql2 0.3.21 ruby lib
# stub: ext/mysql2/extconf.rb

Gem::Specification.new do |s|
  s.name = "mysql2".freeze
  s.version = "0.3.21"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Brian Lopez".freeze]
  s.date = "2016-05-08"
  s.email = "seniorlopez@gmail.com".freeze
  s.extensions = ["ext/mysql2/extconf.rb".freeze]
  s.files = ["ext/mysql2/extconf.rb".freeze]
  s.homepage = "http://github.com/brianmario/mysql2".freeze
  s.licenses = ["MIT".freeze]
  s.rdoc_options = ["--charset=UTF-8".freeze]
  s.rubygems_version = "2.6.3".freeze
  s.summary = "A simple, fast Mysql library for Ruby, binding to libmysql".freeze

  s.installed_by_version = "2.6.3" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake-compiler>.freeze, ["~> 0.9.5"])
      s.add_development_dependency(%q<rake>.freeze, ["~> 0.9.3"])
      s.add_development_dependency(%q<rspec>.freeze, ["~> 2.8.0"])
    else
      s.add_dependency(%q<rake-compiler>.freeze, ["~> 0.9.5"])
      s.add_dependency(%q<rake>.freeze, ["~> 0.9.3"])
      s.add_dependency(%q<rspec>.freeze, ["~> 2.8.0"])
    end
  else
    s.add_dependency(%q<rake-compiler>.freeze, ["~> 0.9.5"])
    s.add_dependency(%q<rake>.freeze, ["~> 0.9.3"])
    s.add_dependency(%q<rspec>.freeze, ["~> 2.8.0"])
  end
end
