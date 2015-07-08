/*
 * Defines general, global scope, functions. 
 */

/**
 * Get the filename from a path.
 * @param {string} path
 * @returns {string}
 */
var getFileName = function (path) {
  var k = path.lastIndexOf("/");
  return path.substr(k + 1);
};