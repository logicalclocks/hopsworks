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

'use strict';

angular.module('hopsWorksApp')

        .factory('MetadataRestService', ['$http', function ($http) {
            var service = {
              
              /**
               * Add a JSON metadata object to an Inode (file/dir)
               * @param {type} user
               * @param {type} parentid
               * @param {type} inodename
               * @param {type} tableid
               * @param {type} data
               * @returns {unresolved}
               */
              addMetadataWithSchema: function (inodepid, inodename, tableid, metadataObj) {
                var jsonObj = JSON.stringify({
                  inodepid: inodepid,
                  inodename: inodename,
                  tableid: tableid,
                  metadata: metadataObj
                });
                var req = {
                  method: 'POST',
                  url: '/api/metadata/addWithSchema',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: jsonObj
                };
                return $http(req);
              },

              
              updateMetadata: function (inodepid, inodename, metadataObj) {
                var jsonObj = JSON.stringify({
                  inodepid: inodepid,
                  inodename: inodename,
                  tableid: -1,
                  metaid: metadataObj.id,
                  metadata: metadataObj
                });
                var req = {
                  method: 'POST',
                  url: '/api/metadata/updateWithSchema',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: jsonObj
                };
                return $http(req);
              },
              


              removeMetadata: function (inodepid, inodename, metadataObj) {
                var jsonObj =  JSON.stringify({
                  inodepid: inodepid,
                  inodename: inodename,
                  tableid: -1, //table id is not necessary when updating metadata
                  metaid: metadataObj.id,                  
                  metadata: metadataObj.data
                });
                var req = {
                  method: 'POST',
                  url: '/api/metadata/removeWithSchema',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: jsonObj
                };
                return $http(req);
              },
              
            };
            return service;
          }]);
