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
 * Change file size from byte to ['KB', ' MB', ' GB', ' TB', 'PB', 'EB', 'ZB', 'YB']
 *  - This gives the actual size of the file not size on disk.
 * @param {long} size in bytes
 */
var convertSize = function (fileSizeInBytes) {
  if (fileSizeInBytes === 0) {
    return 0;
  }
  var i = -1;
  var byteUnits = ['KB', ' MB', ' GB', ' TB', 'PB', 'EB', 'ZB', 'YB'];
  do {
    fileSizeInBytes = fileSizeInBytes / 1024;
    i++;
  } while (fileSizeInBytes > 1024);

  return Math.max(fileSizeInBytes, 0.1).toFixed(1) + byteUnits[i];
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

function getLocationBase() {
  var port = Number(location.port);
  if (port === 'undefined' || port === 0) {
    port = 80;
    if (location.protocol === 'https:') {
      port = 443;
    }
  }

  if (port === 3333 || port === 9000) {
    port = 8080;
  }
  return location.protocol + "//" + location.hostname +":" + port + skipTrailingSlash(location.pathname);
};

function getWsProtocol() {
  return location.protocol === 'https:' ? 'wss:' : 'ws:';
};

function skipTrailingSlash(path) {
  return path.slice(-1) === "/" ? path.substring(0, path.length-1) : path;
}
