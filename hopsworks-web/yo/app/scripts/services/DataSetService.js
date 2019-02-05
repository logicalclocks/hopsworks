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

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('DataSetService', ['$http', function ($http) {
            return function (id) {
              var services = {

                /**
                 * 
                 * @param {type} fileName
                 * @returns json
                 */
               unzip: function (fileName) {
                  return $http.get('/api/project/' + id + '/dataset/unzip/' + fileName);
                },
                /**
                 *
                 * @param {type} fileName
                 * @returns json
                 */
               zip: function (fileName) {
                  return $http.get('/api/project/' + id + '/dataset/zip/' + fileName);
                },
                 
                /**
                 * Get the listing of all datasets under the current project.
                 * @returns {unresolved}
                 */
                getAllDatasets: function () {
                  return $http.get('/api/project/' + id + '/dataset/getContent/');
                },
                /**
                 * Get the contents of the folder to which the path points. 
                 * The parameter is a path relative to the project root folder.
                 * @param {type} relativePath
                 * @returns {unresolved}
                 */
                getContents: function (relativePath) {
                  return $http.get('/api/project/' + id + '/dataset/getContent/' + relativePath);
                },
                
                /**
                 * Checks the existence of a file. Should be caled before fileDownload.
                 * @param {type} fileName is a path relative to the current ds to the file
                 * @returns {unresolved}
                 */
                checkFileExist: function (fileName) {
                  return $http.get('/api/project/' + id + '/dataset/fileExists/' + fileName);
                },
                
                checkFileForDownload: function (fileName) {
                  return $http.get('/api/project/' + id + '/dataset/checkFileForDownload/' + fileName);
                },
                /**
                 * Get file or folder to which the path points. 
                 * The parameter is a path relative to the project root folder.
                 * @param {type} relativePath
                 * @returns {unresolved}
                 */
                getFile: function (relativePath) {
                  return $http.get('/api/project/' + id + '/dataset/getFile/' + relativePath);
                },
                /**
                 * Downloads a file using location.href. This can replace the
                 * page with error page if the download is unsuccessful. So use checkFileExist
                 * before calling this to minimize the risk of an error page being showed. 
                 * @param {type} fileName is a path relative to the current ds to the file 
                 * @returns {undefined}
                 */
                fileDownload: function (fileName, token) {
                  location.href=getPathname() + '/api/project/' + id + '/dataset/fileDownload/' + fileName + '?token=' + token;
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
                permissions: function (dataSet) {
                  var regReq = {
                    method: 'PUT',
                    url: '/api/project/' + id + '/dataset/permissions',
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
                removeImportedDataSetDir: function (fileName) {
                  return $http.delete('/api/project/' + id + '/dataset/removeImportedDataset/' + fileName);
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
                fetchMetadata: function (inodePid, inodeName, tableId) {
                  return $http.get('/api/metadata/fetchmetadata/' + inodePid + '/' + inodeName + '/' + tableId);
                }
              };
              return services;
            };
          }]);
