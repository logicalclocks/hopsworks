/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
'use strict';

angular.module('hopsWorksApp')
        .factory('TourService', ['$timeout',
            function ($timeout) {

                var tourService = this;

                tourService.currentStep_TourOne = -1;
                tourService.currentStep_TourTwo = -1;
                tourService.currentStep_TourThree = -1;
                tourService.currentStep_TourFour = -1;
                tourService.alive_TourOne = 15;
                tourService.createdJobName = null;

                tourService.resetTours = function () {

                    tourService.currentStep_TourOne = -1;
                    tourService.alive_TourOne = 15;
                    tourService.currentStep_TourTwo = -1;
                    tourService.currentStep_TourThree = -1;
                    tourService.currentStep_TourFour = -1;
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
