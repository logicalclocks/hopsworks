/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

/**
 * Controller for the Serving-Info view
 */
angular.module('hopsWorksApp')
    .controller('modelViewInfoCtrl', ['$uibModalInstance', '$scope', '$location', '$window', 'ProjectService',
        'StorageService', 'growl', 'projectId', 'model',
        function ($uibModalInstance, $scope, $location, $window, ProjectService, StorageService, growl, projectId, model) {

            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.model = model;


            self.goToExperiment = function () {
                StorageService.store(self.projectId + "_experiment", model.experimentId);
                $window.open('#!/project/' + self.projectId + '/experiments', '_blank');
            };

            self.goToModelVersion = function () {
                $window.open('#!/project/' + self.projectId + '/datasets/Models/' + model.name + '/' + model.version, '_blank');
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

        }]);

