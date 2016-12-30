#!/usr/bin/env node
'use strict';

var pkg = require('./package.json');
var wiredep = require('wiredep');
var argv = require('minimist')(process.argv.slice(2));
var chalk = require('chalk');
var path = require('path');
var fs = require('fs');
var EOL = require('os').EOL;

// This doesn't take non-named args
delete argv['_'];

var options = {
  help: { short: 'h', desc: 'Print usage information' },
  version: { short: 'v', desc: 'Print the version' },
  bowerJson: { short: 'b', desc: 'Path to `bower.json`' },
  directory: { short: 'd', desc: 'Your Bower directory' },
  exclude: { short: 'e', desc: 'A path to be excluded' },
  ignorePath: { short: 'i', desc: 'A path to be ignored' },
  src: { short: 's', desc: 'Path to your source file' },
  dependencies: { desc: 'Include Bower `dependencies`' },
  devDependencies: { desc: 'Include Bower `devDependencies`' },
  includeSelf: { desc: 'Include top-level `main` files' },
  verbose: { desc: 'Print the results of `wiredep`' }
};

// replace the short forms with the long ones, using the long form as the truth
// also constructs the help text
var helpOptionText = Object.keys(options).map(function (key) {
  var option = options[key];
  var short = option.short;

  // Replace the long value with the short value if it exists
  if (short && argv[short]) {
    argv[key] = argv[short];
    delete argv[short];
  }

  // Construct the help entry
  var line =  ' ' + (short ? '-' + short + ', ' : '') + '--' + key;
  return line + (new Array(22 - line.length)).join(' ') + ' # ' + option.desc;
}).join(EOL);


if (argv.version) {
  console.info(pkg.version);
  return;
}

if (argv.help || !Object.keys(argv).length) {
  console.info(
    pkg.description + EOL +
    EOL +
    'Usage: ' + chalk.cyan('$') + chalk.bold(' wiredep ') +
    chalk.yellow('[options]') + EOL +
    EOL +
    'Options:' + EOL + helpOptionText
  );
  return;
}

if (!argv.src) {
  console.error(
    chalk.bold.red('> Source file not specified.') + EOL +
    'Please pass a `--src path/to/source.html` to `wiredep`.'
  );
  return;
}

if (argv.bowerJson) {
  try {
    argv.bowerJson = require(argv.bowerJson);
  } catch (e) {
    if (e.code === 'MODULE_NOT_FOUND') {
      console.warn(chalk.bold.red('> Could not find `' + argv.bowerJson + '`'));
    }

    if (/SyntaxError/.test(e)) {
      console.warn(chalk.bold.red('> Invalid `' + argv.bowerJson + '`'));
    }

    delete argv.bowerJson;
  }
}

if (!argv.bowerJson) {
  try {
    fs.statSync(path.normalize('./bower.json'));
  } catch (e) {
    console.error(
      chalk.bold.red('> bower.json not found.') + EOL +
      'Please run `wiredep` from the directory where your `bower.json` file' +
      ' is located.' + EOL +
      'Alternatively, pass a `--bowerJson path/to/bower.json`.'
    );
    return;
  }
}

// Replace the arguments short form with their long form names
var results = wiredep(argv);

if (argv.verbose) {
  console.info(results);
}
