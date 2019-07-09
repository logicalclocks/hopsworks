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
 * Controller for the Feature-Info view
 */
angular.module('hopsWorksApp')
    .controller('featureViewInfoCtrl', ['$uibModalInstance', '$scope', 'FeaturestoreService', 'growl', 'projectId',
        'feature', 'featurestore', 'settings',
        function ($uibModalInstance, $scope, FeaturestoreService, growl, projectId, feature, featurestore, settings) {

            /**
             * Initialize controller state
             */
            //Controller Inputs
            var self = this;
            self.projectId = projectId;
            self.feature = feature
            self.featurestore = featurestore;
            self.settings = settings
            //State
            self.pythonCode = ""
            self.scalaCode = ""
            self.table = []
            //Constants
            self.cachedFeaturegroupType = self.settings.cachedFeaturegroupType
            self.onDemandFeaturegroupType = self.settings.onDemandFeaturegroupType

            /**
             * Get the Python API code to retrieve the feature
             */
            self.getPythonCode = function (feature) {
                var codeStr = "from hops import featurestore\n"
                codeStr = codeStr + "featurestore.get_feature('" + feature.name + "')"
                return codeStr
            };

            /**
             * Get the Scala API code to retrieve the feature
             */
            self.getScalaCode = function (feature) {
                var codeStr = "import io.hops.util.Hops\n"
                codeStr = codeStr + "Hops.getFeature('" + feature.name + "').read()"
                return codeStr
            };

            /**
             * Initialization function
             */
            self.init= function () {
                self.pythonCode = self.getPythonCode(self.feature)
                self.scalaCode = self.getScalaCode(self.feature)
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.init()
        }]);

