/*
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
 */

/**
 * Controller for the Serving-Info view
 */
angular.module('hopsWorksApp')
    .controller('servingViewInfoCtrl', ['$uibModalInstance', '$scope', 'ProjectService',
        'growl', 'projectId', 'serving',
        function ($uibModalInstance, $scope, ProjectService, growl, projectId, serving) {

            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.serving = serving;


            /**
             * Initialization function
             */
            self.init= function () {

            };

            /**
             * Gets the Inference Endpoint for a model
             *
             * @param modelName the name of the model
             * @returns the inference endpoint of the model
             */
            self.getInferenceEndpoint = function (modelName) {
                return "https://<hopsworks_server>/hopsworks-api/api/project/id/inference/models/" + modelName + ":<predict|classify|regress>"
            }

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.init()
        }]);

