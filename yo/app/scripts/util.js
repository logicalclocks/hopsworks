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