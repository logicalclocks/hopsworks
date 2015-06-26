
'use strict';

angular.module('hopsWorksApp')
        .controller('FileUploadCtrl', ['$modalInstance', '$scope', 'projectId', 'path', 'growl', function ($modalInstance, $scope, projectId, path, growl) {
            var self = this;
            self.model = {};
            self.projectId = projectId;
            self.path = path;
            self.errorMsg;
            self.files = {};


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
            };

            self.fileErrorHandler = function (file, message, flow) {
              var msg = JSON.parse(message);
              self.errorMsg = msg.errorMsg;
              self.files[file.name] = msg.errorMsg;
            };
            self.errorHandler = function (file, message, flow) {
              var msg = JSON.parse(message);
              growl.error(msg.errorMsg, {title: 'Error', ttl: 5000, referenceId: 1});
            };

            self.fileAddedHandler = function (file, flow) {
              console.log(file.name);
              self.files[file.name] = '';
            };

          }]);
