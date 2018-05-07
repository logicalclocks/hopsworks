/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('TransformGraphCtrl', ['$scope', '$uibModalInstance', '$routeParams', 'growl', '$location', 'ModalService', 'TfServingService', 'servingId', 'inGraph', 'outGraph', 
          function ($scope, $uibModalInstance, $routeParams, growl, $location, ModalService, TfServingService, servingId, inGraph, outGraph) {


            var self = this;
            self.projectId = $routeParams.projectID;
            self.closeAll = false;


            self.servingId = servingId;
            self.transformGraph = {};
            self.in_graph = inGraph;
            self.out_graph = outGraph;
            self.inputs = "";
            self.outputs = "";
            self.transforms = "";


            self.create = function () {
              
              self.transformGraph.in_graph = self.in_graph;
              self.transformGraph.out_graph = self.out_graph;
              self.transformGraph.inputs = self.inputs;
              self.transformGraph.outputs = self.outputs;
              self.transformGraph.transforms = self.transforms;
              
              TfServingService.transformGraph(self.projectId, self.servingId, 
              self.transformGraph).then(
                      function (success) {
//                        self.getAllServings(self.projectId);
                      },
                      function (error) {
                        self.getAllServings(self.projectId);
                        if (error.data !== undefined) {
                          growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                          });
                        }
                      });
                      
              
              
            };
            
            self.clear_inputs = function () {
              self.inputs = "";
            };
            self.add_inputs = function (inputs) {
              self.inputs = self.inputs + " " + inputs;
            };
            self.remove_inputs = function (inputs) {
              self.inputs = self.inputs.replace(" " + inputs, "");
            };

            self.clear_outputs = function () {
              self.outputs = "";
            };
            self.add_outputs = function (outputs) {
              self.outputs = self.outputs + " " + outputs;
            };
            self.remove_outputs = function (outputs) {
              self.outputs = self.outputs.replace(" " + outputs, "");
            };

            self.clear_transform = function () {
              self.transforms = "";
            };
            self.add_transform = function (transform) {
              self.transforms = self.transforms + " " + transform;
            };
            self.remove_transform = function (transform) {
              self.transforms = self.transforms.replace(" " + transform, "");
            };
            
            self.init = function () {
            };

//            self.init();


            self.cancel = function () {
              $uibModalInstance.dismiss('cancel');
            };

          }]);



