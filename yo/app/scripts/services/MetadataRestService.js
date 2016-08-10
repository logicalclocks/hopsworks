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
