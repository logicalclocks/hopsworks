'use strict';

angular.module('hopsWorksApp')
  .factory('ModalService', ['$modal', function ($modal) {
    return {

      confirm: function (size, title, msg) {
        var modalInstance = $modal.open({
          templateUrl: 'views/confirmModal.html',
          controller: 'ModalCtrl as ctrl',
          size: size,
          resolve: {
            title: function () {
              return title;
            },
            msg: function () {
              return msg;
            }
          }
        });
        return modalInstance.result;
      },

      createProject: function (size, title, msg) {
        var modalInstance = $modal.open({
          templateUrl: 'views/projectModal.html',
          controller: 'ProjectCreatorCtrl as projectCreatorCtrl',
          size: size,
          resolve: {
            title: function () {
              return title;
            },
            msg: function () {
              return msg;
            }
          }
        });
        return modalInstance.result;
      },

      projectSettings: function (size, title, msg) {
        var modalInstance = $modal.open({
          templateUrl: 'views/projectSettingsModal.html',
          controller: 'ProjectCtrl as projectCtrl',
          size: size,
          resolve: {
            title: function () {
              return title;
            },
            msg: function () {
              return msg;
            }
          }
        });
        return modalInstance.result;
      }

    }

  }]);
