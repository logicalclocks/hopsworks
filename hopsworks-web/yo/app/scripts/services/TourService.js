/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

'use strict';

angular.module('hopsWorksApp')
        .factory('TourService', ['$timeout',
            function ($timeout) {

                var tourService = this;

                tourService.kafkaSchemaName = "DemoAvroSchema";
                tourService.kafkaTopicName = "DemoKafkaTopic";
                tourService.kafkaProjectPrefix = "demo_kafka";
                tourService.sparkProjectPrefix = "demo_spark";
                tourService.deepLearningProjectPrefix = "demo_deep_learning";
                tourService.deepLearningRunTip = "";
                tourService.informAndTips = true;
                tourService.tipsOnly = false;
                tourService.informOnly = false;
                tourService.showNothing = false;

                tourService.toursInfoStep = 0;
                tourService.currentStep_TourOne = -1;
                tourService.currentStep_TourTwo = -1;
                tourService.currentStep_TourThree = -1;
                tourService.currentStep_TourFour = -1;
                tourService.currentStep_TourFive = -1;
                tourService.currentStep_TourSix = -1;
                tourService.currentStep_TourSeven = -1;
                tourService.currentStep_TourEight = -1;
                tourService.alive_TourOne = 15;
                tourService.createdJobName = null;
                tourService.activeTour = null;
                tourService.kafkaJobCreationState = "producer";
                tourService.counter = 0;

                tourService.getDeepLearningJobTip = function(){
                  if(tourService.activeTour === "deep_learning"){
                    tourService.deepLearningRunTip = "An inference job was created for you! Go on and run it after the model is finished.";
                  }
                  return tourService.deepLearningRunTip;
                };

                tourService.setActiveTour = function (tourName) {
                    tourService.activeTour = tourName;
                };

                tourService.setInformAndTipsState = function () {
                  tourService.informAndTips = true;
                  tourService.tipsOnly = false;
                  tourService.informOnly = false;
                  tourService.showNothing = false;
                };

                tourService.setTipsOnlyState = function () {
                  tourService.informAndTips = false;
                  tourService.tipsOnly = true;
                  tourService.informOnly = false;
                  tourService.showNothing = false;
                };

                tourService.setInformOnly = function () {
                  tourService.informAndTips = false;
                  tourService.tipsOnly = false;
                  tourService.informOnly = true;
                  tourService.showNothing = false;
                };

                tourService.setShowNothingState = function () {
                  tourService.informAndTips = false;
                  tourService.tipsOnly = false;
                  tourService.informOnly = false;
                  tourService.showNothing = true;
                };
                
                tourService.setDefaultTourState = function () {
                  tourService.informAndTips = true;
                  tourService.tipsOnly = false;
                  tourService.informOnly = false;
                  tourService.showNothing = false;
                };

                tourService.printDebug = function () {
                  console.log("Counter: " + tourService.counter);
                  tourService.counter++;
                  console.log(">> kafka state: " + tourService
                  .kafkaJobCreationState);
                  console.log(">> TourSix: " + tourService.currentStep_TourSix);
                  console.log(">> TourSeven: " + tourService
                  .currentStep_TourSeven);

                  return true;
                };

                tourService.getKafkaGuideJarPath = function (projectName, filename) {
                  return "hdfs:///Projects/" + projectName +
                   "/TestJob/" + filename;
                };

                tourService.resetTours = function () {

                    tourService.toursInfoStep = 0;
                    tourService.currentStep_TourOne = -1;
                    tourService.alive_TourOne = 15;
                    tourService.currentStep_TourTwo = -1;
                    tourService.currentStep_TourThree = -1;
                    tourService.currentStep_TourFour = -1;
                    tourService.currentStep_TourFive = -1;
                    tourService.currentStep_TourSix = -1;
                    tourService.currentStep_TourSeven = -1;
                    tourService.currentStep_TourEight = -1;
                    tourService.createdJobName = null;
                };

                tourService.KillTourOneSoon = function ()
                {
                    $timeout(function () {
                        tourService.alive_TourOne--;
                    }, 1000).then(function () {
                        if (tourService.alive_TourOne === 0)
                        {
                            tourService.currentStep_TourOne = -1;
                            tourService.alive_TourOne = 15;
                        } else
                        {
                            tourService.KillTourOneSoon();
                        }
                    });
                };
                return tourService;
            }
        ]);
