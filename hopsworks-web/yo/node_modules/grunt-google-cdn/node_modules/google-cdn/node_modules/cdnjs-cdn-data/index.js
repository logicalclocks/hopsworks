'use strict';

var semver = require('semver');

var data = module.exports = {};

var cdnjs = require('./external/cdnjs.json').results;

cdnjs.forEach(function (item) {
  data[item.name] = {
    versions: item.assets.map(function (asset) {
      return asset.version;
    }).filter(function (version) {
      return semver.valid(version);
    }),
    url: function (version) {
      return ['//cdnjs.cloudflare.com/ajax/libs', item.name, version,
        item.filename].join('/');
    }
  };
});
