#! /usr/bin/env ruby

lib, version, *ignored = ARGV

unless lib
  lib = File.basename(Dir.pwd)
end

unless version
  mod = lib.capitalize
  require "./lib/#{ lib }"
  version = eval(mod).send(:version)
end

abort('no lib') unless lib
abort('no version') unless version

puts "### gemspec: #{ lib }-#{ version }"

$VERBOSE = nil

shiteless = lambda{|list| list.delete_if{|file| file =~ %r/\.(git|svn|tmp|sw.|bak)$/}}

files       = shiteless[Dir::glob("**/**")]
executables = shiteless[Dir::glob("bin/*")].map{|exe| File.basename(exe)}
has_rdoc    = true #File.exist?('doc')
test_files  = "test/#{ lib }.rb" if File.file?("test/#{ lib }.rb")

extensions = []
%w( Makefile configure extconf.rb rakefile Rakefile mkrf_conf ).each do |ext|
  extensions << ext if File.exists?(ext)
end

template = <<-__

  Gem::Specification::new do |spec|
    spec.name = #{ lib.inspect }
    spec.version = #{ version.inspect }
    spec.platform = Gem::Platform::RUBY
    spec.summary = #{ lib.inspect }

    spec.files = #{ files.inspect }
    spec.executables = #{ executables.inspect }
    
    spec.require_path = "lib"

    spec.has_rdoc = #{ has_rdoc.inspect }
    spec.test_files = #{ test_files.inspect }
    #spec.add_dependency 'lib', '>= version'
    #spec.add_dependency 'fattr'

    spec.extensions.push(*#{ extensions.inspect })

    spec.rubyforge_project = 'codeforpeople'
    spec.author = "Ara T. Howard"
    spec.email = "ara.t.howard@gmail.com"
    spec.homepage = "http://github.com/ahoward/#{ lib }/tree/master"
  end

__

puts template
