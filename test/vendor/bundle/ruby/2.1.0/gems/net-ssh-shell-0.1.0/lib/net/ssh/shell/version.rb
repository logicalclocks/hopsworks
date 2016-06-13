require 'net/ssh/version'

module Net; module SSH; module Shell
  # A trivial class for representing the version of this library.
  class Version < Net::SSH::Version
    # The major component of the library's version
    MAJOR = 0

    # The minor component of the library's version
    MINOR = 1

    # The tiny component of the library's version
    TINY  = 0

    # The library's version as a Version instance
    CURRENT = new(MAJOR, MINOR, TINY)

    # The library's version as a String instance
    STRING = CURRENT.to_s
  end
end; end; end

