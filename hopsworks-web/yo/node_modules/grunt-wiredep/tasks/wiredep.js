'use strict';

function wiredep(grunt) {
  grunt.registerMultiTask('wiredep', 'Inject Bower packages into your source code.', function () {
    this.requiresConfig(['wiredep', this.target, 'src']);

    var options = this.options(this.data);
    require('wiredep')(options);
  });
}

module.exports = wiredep;
