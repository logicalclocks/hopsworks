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
    .controller('servingViewInfoCtrl', ['$uibModalInstance', '$scope', '$location', 'ProjectService', 'UtilsService',
        'growl', 'projectId', 'serving', 'isKubernetes',
        function ($uibModalInstance, $scope, $location, ProjectService, UtilsService, growl, projectId, serving, isKubernetes) {

            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.serving = serving;
            self.isKubernetes = isKubernetes;


            /**
             * Initialization function
             */
            self.init= function () {

            };

            /**
             * Constants
             */

            self.servingToolKServe = "KSERVE";
            self.servingToolDefault = "DEFAULT";
            self.modelServerPython = "PYTHON";
            self.modelServerTensorflow = "TENSORFLOW_SERVING";

            /**
             * Gets the Inference Endpoint for a model without predict verb
             *
             * @param serving the serving instance
             * @param internal whether to return an internal endpoint
             * @returns the inference endpoint of the model without the predict verb
             */
            self.getInferenceEndpointWithoutVerb = function (serving, internal) {
                if (internal) {
                    if (typeof serving.internalPort === 'undefined') { return "-" }
                    return self.getInternalHostPort(serving) + serving.internalPath + ":"
                }
                var path = "/hopsworks-api/api/project/" + projectId + "/inference/models/" + serving.name + ":"
                return self.getExternalHostPort(serving) + path
            }

            /**
             * Get external host port
             *
             * @param serving the serving instance
             * @returns external host port
             */
            self.getExternalHostPort = function(serving) {
                var protocol = "http"
                var host = ((typeof serving.externalIP === 'undefined') ? $location.host() : serving.externalIP)
                var port = ((typeof serving.externalPort === 'undefined') ? $location.port() : serving.externalPort)
                return protocol + "://" + host + ":" + port
            }

            /**
             * Get internal host port
             *
             * @param serving the serving instance
             * @returns internal host port
             */
            self.getInternalHostPort = function(serving) {
                var protocol = "http"
                var host = serving.internalIPs[0]
                var port = serving.internalPort
                return protocol + "://" + host + ":" + port
            }

            /**
             * Get predict inference endpoint
             *
             * @param serving the serving instance
             * @param internal whether to return an internal endpoint
             * @returns the predict inference endpoint of the model
             */
            self.getPredictInferenceEndpoint = function(serving, internal) {
                var endpoint = self.getInferenceEndpointWithoutVerb(serving, internal)
                return (endpoint === "-") ? endpoint : endpoint + "predict"
            }

            /**
             * Get classify inference endpoint
             *
             * @param serving the serving instance
             * @param internal whether to return an internal endpoint
             * @returns the classify inference endpoint of the model
             */
            self.getClassifyInferenceEndpoint = function(serving, internal) {
                var endpoint = self.getInferenceEndpointWithoutVerb(serving, internal)
                return (endpoint === "-") ? endpoint : endpoint + "classify"
            }

            /**
             * Get regress inference endpoint
             *
             * @param serving the serving instance
             * @param internal whether to return an internal endpoint
             * @returns the regress inference endpoint of the model
             */
            self.getRegressInferenceEndpoint = function(serving, internal) {
                var endpoint = self.getInferenceEndpointWithoutVerb(serving, internal)
                return (endpoint === "-") ? endpoint : endpoint + "regress"
            }

            /**
             * Convert internal IPs to string format
             *
             * @param internalIPs list of internal IPs
             */
            self.getInternalIPsString = function (internalIPs) {
                return (typeof internalIPs === 'undefined') ? "-" : internalIPs.join(", ")
            }

            /**
             * Convert serving port to string format
             *
             * @param port the port to convert
             */
            self.getInternalPortString = function(port) {
                return (typeof port === 'undefined') ? "-" : port
            }

            /**
             * Get host header value for inference requests to KServe
             *
             * @param serving the serving instance
             */
            self.getKServeHostHeader = function(serving) {
                if (typeof serving.internalPath === 'undefined') { return "-" }
                return serving.name + "." + UtilsService.getProjectName().replaceAll("_", "-") + ".logicalclocks.com"
            }

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.init()
        }]);

