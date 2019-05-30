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
    .controller('servingViewInfoCtrl', ['$uibModalInstance', '$scope', '$location', 'ProjectService',
        'growl', 'projectId', 'serving',
        function ($uibModalInstance, $scope, $location, ProjectService, growl, projectId, serving) {

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
             * Gets the Inference Endpoint for a model without predict verb
             *
             * @param modelName the name of the model
             * @returns the inference endpoint of the model without the predict verb
             */
            self.getInferenceEndpointWithoutVerb = function (modelName) {
                var host = $location.host()
                var port = $location.port()
                var protocol = $location.protocol()
                return protocol + "://" + host + ":" + port + "/hopsworks-api/api/project/" + projectId + "/inference/models/" + modelName + ":"
            }

            /**
             * Get predict inference endpoint
             *
             * @param modelName the name of the model
             * @returns the predict inference endpoint of the model
             */
            self.getPredictInferenceEndpoint = function(modelName) {
                return self.getInferenceEndpointWithoutVerb(modelName) + "predict"
            }

            /**
             * Get classify inference endpoint
             *
             * @param modelName the name of the model
             * @returns the classify inference endpoint of the model
             */
            self.getClassifyInferenceEndpoint = function(modelName) {
                return self.getInferenceEndpointWithoutVerb(modelName) + "classify"
            }

            /**
             * Get regress inference endpoint
             *
             * @param modelName the name of the model
             * @returns the regress inference endpoint of the model
             */
            self.getRegressInferenceEndpoint = function(modelName) {
                return self.getInferenceEndpointWithoutVerb(modelName) + "regress"
            }

            /**
             * Convert serving port to string format
             *
             * @param port the port to convert
             */
            self.getPortString = function(port) {
                if (typeof port === 'undefined' || port === null || port == "") {
                    return "-"
                }
                return port
            }

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.init()
        }]);

