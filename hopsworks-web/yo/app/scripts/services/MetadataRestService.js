/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
