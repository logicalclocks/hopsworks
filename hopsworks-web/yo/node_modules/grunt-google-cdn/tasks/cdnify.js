'use strict';

var path = require('path');
var googlecdn = require('google-cdn');
var bowerConfig = require('bower').config;
var chalk = require('chalk');

module.exports = function (grunt) {

  grunt.registerMultiTask('cdnify', 'Replace scripts with refs to the Google CDN', function () {
    // collect files
    var files = grunt.file.expand({ filter: 'isFile' }, this.data.html);
    var compJson = grunt.file.readJSON('bower.json');
    var options = this.options({
      cdn: 'google'
    });

    // Strip the leading path segment off, e.g. `app/bower_components` ->
    // `bower_components`
    var bowerDirBits = bowerConfig.directory.split(path.sep);
    bowerDirBits.shift();
    var componentsPath = bowerDirBits.join(path.sep);

    grunt.log
      .writeln('Going through ' + grunt.log.wordlist(files) + ' to update script refs');

    files = files.map(function (filepath) {
      return {
        path: filepath,
        body: grunt.file.read(filepath)
      };
    });

    grunt.util.async.forEach(files, function (file, cbInner) {
      var content = file.body;

      content = googlecdn(content, compJson, {
        componentsPath: componentsPath,
        cdn: options.cdn
      }, function (err, content, replacements) {
        if (err) {
          return cbInner(err);
        }

        if (replacements.length > 0) {
          replacements.forEach(function (replacement) {
            grunt.log.writeln(chalk.green('✔ ') + replacement.from + chalk.gray(' changed to ') + replacement.to);
          });
        }

        grunt.file.write(file.path, content);
        cbInner();
      });
    }, this.async());
  });
};
