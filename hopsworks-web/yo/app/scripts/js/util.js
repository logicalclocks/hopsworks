/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
  if (fileSizeInBytes === -1) {
    return "unlimited";
  }
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


var convertNs = function (numFiles) {
  if (numFiles === -1) {
    return "unlimited";
  }
  if (numFiles === 0) {
    return 0;
  }
  var i = -1;
  var byteUnits = ['K', ' M'];
  do {
    numFiles = numFiles / 1000;
    i++;
  } while (numFiles > 1000);

  return Math.max(numFiles, 0.1).toFixed(1) + byteUnits[i];
};

/**
 * Change time in seconds to minutes, hours
 * @param {long} time in seconds
 */
var convertSeconds = function (timeInSeconds) {
  if (timeInSeconds === 0) {
    return 0;
  }
  var mins = timeInSeconds / 60;
  mins = Math.floor(mins);
  if (mins > 60) {
    var hours = mins / 60;
    hours = Math.floor(hours);
    mins = mins - (hours * 60);
    return hours + "hrs " + mins + "mins";
  } else {
    timeInSeconds = timeInSeconds - (mins * 60);
    return mins + "mins " + timeInSeconds + "secs";    
  }

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

//w3school get cookie example
function getCookie(cname) {
    var name = cname + '=';
    var ca = document.cookie.split(';');
    for(var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
}

function getPort() {
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
  return port;
};

function getPathname() {
  return "/hopsworks-api";
}

function getApiPath() {
  return "/hopsworks-api/api";
}

function getLocationBase() {
  return location.protocol + "//" + location.hostname +":" + getPort() + "/hopsworks";
};

function getApiLocationBase() {
  return location.protocol + "//" + location.hostname +":" + getPort() + getPathname();
};

function getWsProtocol() {
  return location.protocol === 'https:' ? 'wss:' : 'ws:';
};

function getMetaDataWsBaseURL() {
  return getWsProtocol() +"//" + location.hostname + ":" + getPort() + getPathname() + "/wspoint/";
}

function getZeppelinWsBaseURL() {
  return getWsProtocol() +"//" + location.hostname + ":" + getPort() + getPathname() + "/zeppelin/ws/";
};

function skipTrailingSlash(path) {
  return path.slice(-1) === "/" ? path.substring(0, path.length-1) : path;
};

jQuery(document).on('click', '.mega-dropdown', function(e) {
  e.stopPropagation();
});
