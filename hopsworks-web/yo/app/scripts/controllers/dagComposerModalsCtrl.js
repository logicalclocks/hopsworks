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
'use strict';
angular.module('hopsWorksApp')
    .controller('DagComposerModalsCtrl', ['$uibModalInstance', 'growl', 'operator', 'jobs', 'addedOperators',
        function($uibModalInstance, growl, operator, jobs, addedOperators) {
            self = this;
            self.operator = operator;
            self.jobs = jobs;
            self.jobNames = [];
            self.addedOperators = addedOperators;
            self.jobCanTakeArguments = false;

            self.tmpDagDefinition = {
                dependsOn: []
            };

            self.addOperator2Dag = function() {
                if (!self.operator.hasAttribute(self.operator.HAS_JOB_NAME_ATTR)
                    && (self.isUndefined(self.operator.id) || self.operator.id == "")) {
                    var errorMsg = "Operator name is required";
                } else if (self.operator.hasAttribute(self.operator.HAS_JOB_NAME_ATTR)
                    && (self.isUndefined(self.operator.jobName) || self.operator.jobName == "")) {
                        var errorMsg = "Job name to operate is required";
                } else if (self.operator.hasAttribute(self.operator.HAS_WAIT_2_FINISH_ATTR)
                    && self.isUndefined(self.operator.wait)) {
                        var errorMsg = "Wait for job to finish flag is required";
                } else if (self.operator.hasAttribute(self.operator.HAS_FEATURE_GROUPS_NAME_ATTR)
                    && (self.isUndefined(self.operator.featureGroupName) || self.operator.featureGroupName == "")) {
                        var errorMsg = "Feature group name is required";
                }
                if (self.isUndefined(errorMsg)) {
                    if (self.operator.type == 0) {
                        self.operator.id = "launch_" + self.operator.jobName;
                    } else if (self.operator.type == 1) {
                        self.operator.id = "wait_" + self.operator.jobName;
                    }
                    growl.info("Operator " + self.operator.id + " successfully added to DAG",
                        {title: "Added operator", ttl: 2000, referenceId: "dag_comp_growl"});
                    self.operator.dependsOn = [];
                    for (var idx = 0; idx < self.tmpDagDefinition.dependsOn.length; idx++) {
                        self.operator.dependsOn[idx] = self.tmpDagDefinition.dependsOn[idx].id;
                    }
                    $uibModalInstance.close(self.operator);
                } else {
                    growl.error(errorMsg,
                        {title: "Failed to add operator", ttl: 5000, referenceId: "dag_comp_growl"});
                        $uibModalInstance.dismiss('cancel');
                }
            }

            self.init = function () {
                self.jobs.forEach(function (j) {
                    self.jobNames.push(j.name);
                });
            }

            self.isUndefined = function(input) {
                return typeof input === "undefined";
            }

            self.close = function() {
                $uibModalInstance.dismiss('cancel');
            }

            self.checkIfJobCanTakeArguments = function () {
                var selectedJob = self.jobs.find(function (j) {
                    return j.name == self.operator.jobName;
                });
                if(selectedJob != null) {
                    if(selectedJob.jobType.toUpperCase() !== 'FLINK') {
                        self.jobCanTakeArguments = true;
                    }
                }
            }

            self.init();
        }
    ]);