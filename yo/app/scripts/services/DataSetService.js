/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('DataSetService', ['$http', function ($http) {
            return function (id) {
              var services = {
                /**
                 * Get the listing of all datasets under the current project.
                 * @returns {unresolved}
                 */
                getAllDatasets: function () {
                  return $http.get('/api/project/' + id + '/dataset/');
                },
                /**
                 * Get the contents of the folder to which the path points. 
                 * The parameter is a path relative to the project root folder.
                 * @param {type} relativePath
                 * @returns {unresolved}
                 */
                getContents: function (relativePath) {
                  return $http.get('/api/project/' + id + '/dataset/' + relativePath);
                },
                /**
                 * Checks the existence of a file. Should be caled before fileDownload.
                 * @param {type} fileName is a path relative to the current ds to the file
                 * @returns {unresolved}
                 */
                checkFileExist: function (fileName) {
                  return $http.get('/api/project/' + id + '/dataset/fileExists/' + fileName);
                },
                /**
                 * Downloads a file using location.href. This can replace the
                 * page with error page if the download is unsuccessful. So use checkFileExist
                 * before calling this to minimize the risk of an error page being showed. 
                 * @param {type} fileName is a path relative to the current ds to the file 
                 * @returns {undefined}
                 */
                fileDownload: function (fileName) {
                  location.href='/hopsworks/api/project/' + id + '/dataset/fileDownload/' + fileName;
                },
                compressFile: function(fileName) {
                  return $http.get('/api/project/' + id + '/dataset/compressFile/' + fileName);
                },
                checkFileBlocks: function(fileName){
                  return $http.get('/api/project/' + id + '/dataset/countFileBlocks/' + fileName);
                },
                isDir: function(path){
                  return $http.get('/api/project/' + id + '/dataset/isDir/' + path);
                },
                upload: function (dataSetPath) {
                  return $http.post('/api/project/' + id + '/dataset/upload/' + dataSetPath);
                },
                createDataSetDir: function (dataSet) {
                  var regReq = {
                    method: 'POST',
                    url: '/api/project/' + id + '/dataset',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: dataSet
                  };

                  return $http(regReq);
                },
                createTopLevelDataSet: function (dataSet) {
                  var regReq = {
                    method: 'POST',
                    url: '/api/project/' + id + '/dataset/createTopLevelDataSet',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: dataSet
                  };

                  return $http(regReq);
                },
                shareDataSet: function (dataSet) {
                  var regReq = {
                    method: 'POST',
                    url: '/api/project/' + id + '/dataset/shareDataSet',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: dataSet
                  };

                  return $http(regReq);
                },
                acceptDataset: function (inodeId) {
                  return $http.get('/api/project/' + id + '/dataset/accept/' + inodeId);
                },
                rejectDataset: function (inodeId) {
                  return $http.get('/api/project/' + id + '/dataset/reject/' + inodeId);
                },
                removeDataSetDir: function (fileName) {
                  return $http.delete('/api/project/' + id + '/dataset/' + fileName);
                },
                attachTemplate: function (fileTemplateData) {
                  var regReq = {
                    method: 'POST',
                    url: '/api/project/' + id + '/dataset/attachTemplate',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: fileTemplateData
                  };

                  return $http(regReq);
                },
                fetchTemplate: function(templateid, sender){
                  return $http.get('/api/metadata/fetchtemplate/' + templateid + '/' + sender);
                },
                fetchTemplatesForInode: function(inodeid){
                  return $http.get('/api/metadata/fetchtemplatesforinode/' + inodeid);
                },
                fetchAvailableTemplatesforInode: function(inodeid){
                  return $http.get('/api/metadata/fetchavailabletemplatesforinode/' + inodeid);
                },
                detachTemplate: function(inodeid, templateid){
                  return $http.get('/api/metadata/detachtemplate/' + inodeid + '/' + templateid);
                },
                fetchMetadata: function (inodePid, inodeName, tableId) {
                  return $http.get('/api/metadata/fetchmetadata/' + inodePid + '/' + inodeName + '/' + tableId);
                }
              };
              return services;
            };
          }]);
