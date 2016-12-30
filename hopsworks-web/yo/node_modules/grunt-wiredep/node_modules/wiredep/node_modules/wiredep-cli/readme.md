# wiredep-cli [![npm](https://badge.fury.io/js/wiredep-cli.svg)](http://badge.fury.io/js/wiredep-cli) [![Build Status](https://travis-ci.org/taptapship/wiredep-cli.svg?branch=master)](https://travis-ci.org/taptapship/wiredep-cli)
> [Wiredep](https://github.com/taptapship/wiredep) CLI interface

## Getting Started
Install the module with [npm](https://npmjs.org):

```bash
$ npm install --save wiredep-cli
```

## Command Line
Install wiredep globally to wire up Bower packages from the terminal.

```sh
$ npm install -g wiredep
$ wiredep
Wire Bower dependencies to your source code.

Usage: $ wiredep [options]

Options:
  -h, --help          # Print usage information
  -v, --version       # Print the version
  -b, --bowerJson     # Path to `bower.json`
  -d, --directory     # Your Bower directory
  -e, --exclude       # A path to be excluded
  -i, --ignorePath    # A path to be ignored
  -s, --src           # Path to your source file
  --dependencies      # Include Bower `dependencies`
  --devDependencies   # Include Bower `devDependencies`
  --includeSelf       # Include top-level bower.json `main` files
  --verbose           # Print the results of `wiredep`
```

### Bower Hooks
You may also take advantage of Bower >=1.3.1's [hooks](https://github.com/bower/bower/blob/master/HOOKS.md), with a `.bowerrc` such as:

```json
{
  "scripts": {
    "postinstall": "wiredep -s path/to/src.html"
  }
}
```


## Configuration

See [Wiredep](https://github.com/taptapship/wiredep) for the available options

## Contributing
In lieu of a formal styleguide, take care to maintain the existing coding style. Add unit tests for any new or changed functionality. Lint and test your code using `npm test`.


## License
Copyright (c) 2016 Stephen Sawchuk. Licensed under the MIT license.
