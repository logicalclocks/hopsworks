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
                  location.href=getPathname() + '/api/project/' + id + '/dataset/fileDownload/' + fileName;
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
                unshareDataSet: function (dataSet) {
                  var regReq = {
                    method: 'POST',
                    url: '/api/project/' + id + '/dataset/unshareDataSet',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: dataSet
                  };

                  return $http(regReq);
                },
                projectsSharedWith: function (dataSet) {
                  var regReq = {
                    method: 'POST',
                    url: '/api/project/' + id + '/dataset/projectsSharedWith',
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
                filePreview: function (filePath, mode) {
                  return $http.get('/api/project/' + id + '/dataset/filePreview/' + filePath +"?mode="+mode);
                },
                move: function (srcInodeId, fullPath) {
                  
                  var moveOp = { 
                    inodeId: srcInodeId, 
                    destPath: fullPath 
                  };

                  var moveReq = {
                    method: 'POST',
                    url: '/api/project/' + id + '/dataset/move',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: moveOp
                  };
                  return $http(moveReq);                  
                },
                copy: function (srcInodeId, fullPath) {
                  var copyOp = { 
                    inodeId: srcInodeId, 
                    destPath: fullPath 
                  };

                  var copyReq = {
                    method: 'POST',
                    url: '/api/project/' + id + '/dataset/copy',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: copyOp
                  };
                  return $http(copyReq);                  
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
                makePublic: function (inodeId) {
                  return $http.get('/api/project/' + id + '/dataset/makePublic/' + inodeId);
                },             
                removePublic: function (inodeId) {
                  return $http.get('/api/project/' + id + '/dataset/removePublic/' + inodeId);
                },             
                fetchMetadata: function (inodePid, inodeName, tableId) {
                  return $http.get('/api/metadata/fetchmetadata/' + inodePid + '/' + inodeName + '/' + tableId);
                },
                getReadme: function(path) {
                  return $http.get('/api/project/readme/' + path);
                }
              };
              return services;
            };
          }]);
