'use strict';
var chalk = require('chalk');
var eachAsync = require('each-async');
var prettyBytes = require('pretty-bytes');
var logSymbols = require('log-symbols');
var SVGO = require('svgo');

module.exports = function (grunt) {
	grunt.registerMultiTask('svgmin', 'Minify SVG', function () {
		var done = this.async();
		var svgo = new SVGO(this.options());
		var totalSaved = 0;

		eachAsync(this.files, function (el, i, next) {
			var srcPath = el.src[0];
			var srcSvg = grunt.file.read(srcPath);

			svgo.optimize(srcSvg, function (result) {
				if (result.error) {
					grunt.warn(srcPath + ': ' + result.error);
					next();
					return;
				}

				var saved = srcSvg.length - result.data.length;
				var percentage = saved / srcSvg.length * 100;
				totalSaved += saved;

				grunt.verbose.writeln(logSymbols.success + ' ' + srcPath + chalk.gray(' (saved ' + chalk.bold(prettyBytes(saved)) + ' ' + Math.round(percentage) + '%)'));
				grunt.file.write(el.dest, result.data);
				next();
			});
		}, function () {
			grunt.log.writeln('Total saved: ' + chalk.green(prettyBytes(totalSaved)));
			done();
		});
	});
};
