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
