
'use strict';

angular.module('hopsWorksApp')
        .controller('FileUploadCtrl', ['$modalInstance', '$scope', 'projectId', 'path', function ($modalInstance, $scope, projectId, path) {
            var self = this;
            self.model = {};
            self.projectId = projectId;
            self.path = path;

            self.size = function (fileSizeInBytes) {
              if (fileSizeInBytes == 0) {
                return 0;
              }
              var i = -1;
              var byteUnits = [' kB', ' MB', ' GB', ' TB', 'PB', 'EB', 'ZB', 'YB'];
              do {
                fileSizeInBytes = fileSizeInBytes / 1024;
                i++;
              } while (fileSizeInBytes > 1024);

              return Math.max(fileSizeInBytes, 0.1).toFixed(1) + byteUnits[i];
            }

          }]);
