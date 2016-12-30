'use strict';

var $ = {
  _: require('lodash'),
  'bower-config': require('bower-config'),
  fs: require('fs'),
  glob: require('glob'),
  lodash: require('lodash'),
  path: require('path'),
  through2: require('through2')
};

var config;
var helpers = require('./lib/helpers');
var fileTypesDefault = require('./lib/default-file-types');

/**
 * Wire up the html files with the Bower packages.
 *
 * @param  {object} config  the global configuration object
 */
function wiredep(opts) {
  opts = opts || {};

  var cwd = opts.cwd ? $.path.resolve(opts.cwd) : process.cwd();

  config = module.exports.config = helpers.createStore();

  config.set
    ('on-error', opts.onError || function(err) { throw new Error(err); })
    ('on-file-updated', opts.onFileUpdated || function() {})
    ('on-main-not-found', opts.onMainNotFound || function() {})
    ('on-path-injected', opts.onPathInjected || function() {});

  config.set
    ('bower.json', opts.bowerJson || JSON.parse($.fs.readFileSync($.path.join(cwd, './bower.json'))))
    ('bower-directory', opts.directory || findBowerDirectory(cwd))
    ('cwd', cwd)
    ('dependencies', opts.dependencies === false ? false : true)
    ('detectable-file-types', [])
    ('dev-dependencies', opts.devDependencies)
    ('exclude', Array.isArray(opts.exclude) ? opts.exclude : [ opts.exclude ])
    ('file-types', mergeFileTypesWithDefaults(opts.fileTypes))
    ('global-dependencies', helpers.createStore())
    ('ignore-path', opts.ignorePath)
    ('include-self', opts.includeSelf)
    ('overrides', $._.extend({}, config.get('bower.json').overrides, opts.overrides))
    ('src', [])
    ('stream', opts.stream ? opts.stream : {});

  $._.map(config.get('file-types'), 'detect').
    forEach(function (fileType) {
      Object.keys(fileType).
        forEach(function (detectableFileType) {
          var detectableFileTypes = config.get('detectable-file-types');

          if (detectableFileTypes.indexOf(detectableFileType) === -1) {
            config.set('detectable-file-types', detectableFileTypes.concat(detectableFileType));
          }
        });
    });

  if (!opts.stream && opts.src) {
    (Array.isArray(opts.src) ? opts.src : [opts.src]).
      forEach(function (pattern) {
        config.set('src', config.get('src').concat($.glob.sync(pattern)));
      });
  }

  require('./lib/detect-dependencies')(config);
  require('./lib/inject-dependencies')(config);

  return config.get('stream').src ||
    Object.keys(config.get('global-dependencies-sorted')).
      reduce(function (acc, depType) {
        if (config.get('global-dependencies-sorted')[depType].length) {
          acc[depType] = config.get('global-dependencies-sorted')[depType];
        }

        return acc;
      }, { packages: config.get('global-dependencies').get() });
}

function mergeFileTypesWithDefaults(optsFileTypes) {
  var fileTypes = $._.clone(fileTypesDefault, true);

  $._(optsFileTypes).each(function (fileTypeConfig, fileType) {
    // fallback to the default type for all html-like extensions (php, twig, hbs, etc)
    fileTypes[fileType] = fileTypes[fileType] || fileTypes['default'];
    $._.each(fileTypeConfig, function (config, configKey) {
      if ($._.isPlainObject(fileTypes[fileType][configKey])) {
        fileTypes[fileType][configKey] =
          $._.assign(fileTypes[fileType][configKey], config);
      } else {
        fileTypes[fileType][configKey] = config;
      }
    });
  });

  return fileTypes;
}

function findBowerDirectory(cwd) {
  var directory = $.path.join(cwd, ($['bower-config'].read(cwd).directory || 'bower_components'));

  if (!$.fs.existsSync(directory)) {
    var error = new Error('Cannot find where you keep your Bower packages.');
    error.code = 'BOWER_COMPONENTS_MISSING';
    config.get('on-error')(error);
  }

  return directory;
}

wiredep.stream = function (opts) {
  opts = opts || {};

  return $.through2.obj(function (file, enc, cb) {
    if (file.isNull()) {
      this.push(file);
      return cb();
    }

    if (file.isStream()) {
      this.emit('error', 'Streaming not supported');
      return cb();
    }

    try {
      opts.stream = {
        src: file.contents.toString(),
        path: file.path,
        fileType: $.path.extname(file.path).substr(1)
      };

      file.contents = new Buffer(wiredep(opts));
    } catch (err) {
      this.emit('error', err);
    }

    this.push(file);
    cb();
  });
};


module.exports = wiredep;
