/*
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
 *
 */

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('LocalFsService', ['$http', function ($http) {
            return function (id) {
              var services = {
                /**
                 * Get the listing of all datasets under the current project.
                 * @returns {unresolved}
                 */
                getAllFiles: function () {
                  return $http.get('/api/project/' + id + '/localfs/');
                },
                /**
                 * Get the contents of the folder to which the path points. 
                 * The parameter is a path relative to the project root folder.
                 * @param {type} relativePath
                 * @returns {unresolved}
                 */
                getContents: function (relativePath) {
                  return $http.get('/api/project/' + id + '/localfs/' + relativePath);
                },
                /**
                 * Checks the existence of a file. Should be caled before fileDownload.
                 * @param {type} fileName is a path relative to the current ds to the file
                 * @returns {unresolved}
                 */
                checkFileExist: function (fileName) {
                  return $http.get('/api/project/' + id + '/localfs/fileExists/' + fileName);
                },
                isDir: function(path){
                  return $http.get('/api/project/' + id + '/localfs/isDir/' + path);
                },
                createLocalDir: function (dirName) {
                  var regReq = {
                    method: 'POST',
                    url: '/api/project/' + id + '/localfs',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: dirName
                  };

                  return $http(regReq);
                },
                removeLocalDir: function (fileName) {
                  return $http.delete('/api/project/' + id + '/localfs/' + fileName);
                }
              };
              return services;
            };
          }]);
