'use strict';
var debug = require('debug')('fileprocessor');
var File = require('./file');
var _ = require('lodash');
var chalk = require('chalk');

var _defaultPatterns = {
  html: [
    [
      /<script.+src=['"]([^"']+)["']/gm,
      'Update the HTML to reference our concat/min/revved script files'
    ],
    [
      /<link[^\>]+href=['"]([^"']+)["']/gm,
      'Update the HTML with the new css filenames'
    ],
    [
      /<img[^\>]*[^\>\S]+src=['"]([^'"\)#]+)(#.+)?["']/gm,
      'Update the HTML with the new img filenames'
    ],
    [
      /<video[^\>]+src=['"]([^"']+)["']/gm,
      'Update the HTML with the new video filenames'
    ],
    [
      /<video[^\>]+poster=['"]([^"']+)["']/gm,
      'Update the HTML with the new poster filenames'
    ],
    [
      /<source[^\>]+src=['"]([^"']+)["']/gm,
      'Update the HTML with the new source filenames'
    ],
    [
      /data-main\s*=['"]([^"']+)['"]/gm,
      'Update the HTML with data-main tags',
      function (m) {
        return m.match(/\.js$/) ? m : m + '.js';
      },
      function (m) {
        return m.replace('.js', '');
      }
    ],
    [
      /data-(?!main).[^=]+=['"]([^'"]+)['"]/gm,
      'Update the HTML with data-* tags'
    ],
    [
      /url\(\s*['"]?([^"'\)]+)["']?\s*\)/gm,
      'Update the HTML with background imgs, case there is some inline style'
    ],
    [
      /<a[^\>]+href=['"]([^"']+)["']/gm,
      'Update the HTML with anchors images'
    ],
    [
      /<input[^\>]+src=['"]([^"']+)["']/gm,
      'Update the HTML with reference in input'
    ],
    [
      /<meta[^\>]+content=['"]([^"']+)["']/gm,
      'Update the HTML with the new img filenames in meta tags'
    ],
    [
      /<object[^\>]+data=['"]([^"']+)["']/gm,
      'Update the HTML with the new object filenames'
    ],
    [
      /<image[^\>]*[^\>\S]+xlink:href=['"]([^"'#]+)(#.+)?["']/gm,
      'Update the HTML with the new image filenames for svg xlink:href links'
    ],
    [
      /<image[^\>]*[^\>\S]+src=['"]([^'"\)#]+)(#.+)?["']/gm,
      'Update the HTML with the new image filenames for src links'
    ],
    [
      /<(?:img|source)[^\>]*[^\>\S]+srcset=['"]([^"'\s]+)\s*?(?:\s\d*?[w])?(?:\s\d*?[x])?\s*?["']/gm,
      'Update the HTML with the new image filenames for srcset links'
    ],
    [
      /<(?:use|image)[^\>]*[^\>\S]+xlink:href=['"]([^'"\)#]+)(#.+)?["']/gm,
      'Update the HTML within the <use> tag when referencing an external url with svg sprites as in svg4everybody'
    ]
  ],
  css: [
    [
      /(?:src=|url\(\s*)['"]?([^'"\)(\?|#)]+)['"]?\s*\)?/gm,
      'Update the CSS to reference our revved images'
    ]
  ],
  json: [
    [
      /:\s*['"]([^"']+)["']/gm,
      'Update the json value to reference our revved url'
    ]
  ],
  svg: [
    [
      /<image[^\>]*[^\>\S]+xlink:href=['"]([^"'#]+)(#.+)?["']/gm,
      'Update the SVG with the new image filenames for svg xlink:href links'
    ]
  ]
};

//
// Default block replacement functions, for css and js types
//
var defaultBlockReplacements = {
  css: function (block) {
    var media = block.media ? ' media="' + block.media + '"' : '';
    return '<link rel="stylesheet" href="' + block.dest + '"' + media + '>';
  },
  js: function (block) {
    var defer = block.defer ? 'defer ' : '';
    var async = block.async ? 'async ' : '';
    return '<script ' + defer + async + 'src="' + block.dest + '"><\/script>';
  }
};

var FileProcessor = module.exports = function (type, patterns, finder, logcb, blockReplacements) {
  if (!type) {
    throw new Error('No type given');
  }

  if (!_.isArray(patterns)) {
    throw new Error('Patterns must be an array');
  }

  this.patterns = _defaultPatterns[type] || [];

  if (patterns.length) {
    this.patterns = this.patterns.concat(patterns);
  }

  this.log = logcb || function () {};

  if (!finder) {
    throw new Error('Missing parameter: finder');
  }
  this.finder = finder;


  this.blockReplacements = _.assign({}, defaultBlockReplacements);
  if (blockReplacements && _.keys(blockReplacements).length > 0) {
    _.assign(this.blockReplacements, blockReplacements);
  }

};

//
// Replace blocks by their target
//
FileProcessor.prototype.replaceBlocks = function replaceBlocks(file) {
  var result = file.content;
  var linefeed = /\r\n/g.test(result) ? '\r\n' : '\n';
  file.blocks.forEach(function (block) {
    var blockLine = block.raw.join(linefeed);
    result = result.replace(blockLine, this.replaceWith(block));
  }, this);
  return result;
};


FileProcessor.prototype.replaceWith = function replaceWith(block) {
  var result;
  var conditionalStart = block.conditionalStart ? block.conditionalStart + '\n' + block.indent : '';
  var conditionalEnd = block.conditionalEnd ? '\n' + block.indent + block.conditionalEnd : '';
  if (typeof block.src === 'undefined' || block.src === null || block.src.length === 0) {
    // there are no css or js files in the block, remove it completely
    result = '';
  } else if (_.contains(_.keys(this.blockReplacements), block.type)) {
    result = block.indent + conditionalStart + this.blockReplacements[block.type](block) + conditionalEnd;
  } else {
    result = '';
  }
  return result;
};

//
// Replace reference to scripts, css, images, ... in +lines+ with their revved version
// If +lines+ is not furnished, use instead the cached version (i.e. stored at constructor time)
//
FileProcessor.prototype.replaceWithRevved = function replaceWithRevved(lines, assetSearchPath) {
  // Replace script sources
  var self = this;
  var content = lines;
  var regexps = this.patterns;
  var identity = function (m) {
    return m;
  };

  // Replace reference to script with the actual name of the revved script
  regexps.forEach(function (rxl) {
    var filterIn = rxl[2] || identity;
    var filterOut = rxl[3] || identity;

    self.log(rxl[1]);
    content = content.replace(rxl[0], function (match, src) {
      // Consider reference from site root
      var srcFile = filterIn(src);

      debug('Let\'s replace ' + src);

      debug('Looking for revved version of ' + srcFile + ' in ', assetSearchPath);

      var file = self.finder.find(srcFile, assetSearchPath);

      debug('Found file \'%s\'', file);

      if (src === '/') {
        return match;
      }

      var res = match.replace(src, filterOut(file));
      if (srcFile !== file) {
        self.log(chalk.cyan(src) + ' changed to ' + chalk.cyan(file));
      }
      return res;
    });
  });

  return content;
};


FileProcessor.prototype.process = function (filename, assetSearchPath) {
  debug('processing file %s', filename, assetSearchPath);

  if (_.isString(filename)) {
    this.file = new File(filename);
  } else {
    // filename is an object and should conform to lib/file.js API
    this.file = filename;
  }

  if (assetSearchPath && assetSearchPath.length !== 0) {
    this.file.searchPath = assetSearchPath;
  }

  var content = this.replaceWithRevved(this.replaceBlocks(this.file), this.file.searchPath);

  return content;
};
