'use strict';

var async = require('async');
var semver = require('semver');
var debug = require('debug')('google-cdn');
var requote = require('regexp-quote');

var data = {
  google: require('google-cdn-data'),
  cdnjs: require('cdnjs-cdn-data')
};

var bowerUtil = require('./util/bower');


function getVersionStr(bowerJson, name) {
  var versionStr;
  if (bowerJson.dependencies) {
    versionStr = bowerJson.dependencies[name];
  }

  if (!versionStr && bowerJson.devDependencies && bowerJson.devDependencies[name]) {
    versionStr = bowerJson.devDependencies[name];
  }

  return versionStr;
}


function isFunction(fn) {
  return typeof(fn) === 'function';
}


module.exports = function cdnify(content, bowerJson, options, callback) {

  if (!bowerJson) {
    throw new Error('Required argument `bowerJson` is missing.');
  }

  if (isFunction(options)) {
    callback = options;
    options = {};
  }

  options = options || {};
  options.componentsPath = options.componentsPath || 'bower_components';

  var cdn = options.cdn || 'google';
  var cdnData = (typeof(cdn) === 'object' ? cdn : data[cdn]);

  if (!cdnData) {
    return callback(new Error('CDN ' + cdn + ' is not supported.'));
  }

  function generateReplacement(bowerPath, url) {
    // Replace leading slashes if present.
    var from = bowerUtil.joinComponent(options.componentsPath, bowerPath);
    var fromRe = '/?' + requote(from);
    var fromRegex = new RegExp(fromRe);
    return {
      from: from,
      fromRegex: fromRegex,
      to: url
    };
  }

  function buildReplacement(name, callback) {
    var item = cdnData[name];
    var versionStr = getVersionStr(bowerJson, name);

    if (!versionStr) {
      return callback();
    }

    var version = semver.maxSatisfying(item.versions, versionStr);
    if (version) {
      var url = (isFunction(item.url)) ? item.url(version) : item.url;
      debug('Choosing version %s for dependency %s', version, name);

      if (item.all) {
        callback(null, generateReplacement(name, url));
      } else {
        bowerUtil.resolveMainPath(name, versionStr, function (err, main) {
          if (err) {
            return callback(err);
          } else {
            callback(null, generateReplacement(main, url));
          }
        });
      }
    } else {
      debug('Could not find satisfying version for %s %s', name, versionStr);
      callback();
    }
  }

  async.map(Object.keys(cdnData), buildReplacement, function (err, replacements) {
    if (err) {
      return callback(err);
    }

    var replacementInfo = [];

    replacements.forEach(function (replacement) {
      if (replacement) {
        content = content.replace(replacement.fromRegex, replacement.to);
        debug('Replaced %s with %s', replacement.fromRegex, replacement.to);
        replacementInfo.push(replacement);
      }
    });

    callback(null, content, replacementInfo);
  });
};
