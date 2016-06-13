## session.gemspec
#

Gem::Specification::new do |spec|
  spec.name = "session"
  spec.version = "3.2.0"
  spec.platform = Gem::Platform::RUBY
  spec.summary = "session"
  spec.description = "persistent connections with external programs like bash"
  spec.license = "same as ruby's"

  spec.files =
["LICENSE",
 "README",
 "Rakefile",
 "gemspec.rb",
 "lib",
 "lib/session.rb",
 "sample",
 "sample/bash.rb",
 "sample/bash.rb.out",
 "sample/driver.rb",
 "sample/session_idl.rb",
 "sample/session_sh.rb",
 "sample/sh0.rb",
 "sample/stdin.rb",
 "sample/threadtest.rb",
 "session.gemspec",
 "test",
 "test/session.rb"]

  spec.executables = []
  
  spec.require_path = "lib"

  spec.test_files = "test/session.rb"

  

  spec.extensions.push(*[])

  spec.rubyforge_project = "codeforpeople"
  spec.author = "Ara T. Howard"
  spec.email = "ara.t.howard@gmail.com"
  spec.homepage = "https://github.com/ahoward/session"
end
