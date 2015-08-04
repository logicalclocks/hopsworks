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

/**
 * Sorts an object (a list) based on a predicate
 * 
 * @param {type} filter
 * @param {type} predicate
 * @param {type} template
 * @returns {sorted list}
 */
var sortObject = function(filter, predicate, template){
  
  //false means 'do not reverse the order of the array'
  return filter('orderBy')(template.columns, predicate, false);
};