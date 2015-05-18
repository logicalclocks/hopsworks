/**
 * Created by AMore on 2015-04-24.
 */

'use strict';


angular.module('hopsWorksApp')
  .controller('DatasetsCtrl', ['$rootScope', '$modal', '$scope', '$timeout', '$mdSidenav', '$mdUtil', '$log', 'WSComm',
        'DataSetService', '$routeParams','ModalService','growl','ProjectService',
    function ($rootScope, $modal, $scope, $timeout, $mdSidenav, $mdUtil, $log, WSComm, DataSetService, $routeParams, ModalService, growl, ProjectService) {

      var self = this;

      self.datasets = [];
      self.currentDataSet = "";
      self.currentProject = "";

      self.dataSet = {};
      var file = {name: "", owner: 'André', modified: "", filesize: '4 GB', path:"", dir:""};
      self.files = [file];
      var pId = $routeParams.projectID;
      var currentDS =  $routeParams.datasetName;
      var dataSetService = DataSetService(pId);
        //this can be removed if we use project name instead of project id
        ProjectService.get({}, {'id': pId}).$promise.then(
            function (success) {
                console.log(success);
                self.currentProject = success;
            }, function (error) {
                $location.path('/');
            }
        );
        if(currentDS){
            self.currentDataSet = currentDS;
        }

        var getAll = function () {
           dataSetService.getAll().then (
               function (success) {
                   self.datasets=success.data;
                   console.log(success);
               }, function (error){
                   console.log("getAll error");
                   console.log(error);
               });
       };
        var getDir = function (name) {
            dataSetService.getDir(name).then (
                function (success) {
                    self.files = success.data;
                    console.log(success);
                }, function (error){
                    console.log("getDir error");
                    console.log(error);
                });
        };
        var download = function (file) {
            dataSetService.download(file).then (
                function (success) {
                    console.log("download success");
                    console.log(success);
                }, function (error){
                    console.log("download error");
                    console.log(error);
                });
        };
        var upload = function (path) {
            dataSetService.upload(path).then (
                function (success) {
                    console.log("upload success");
                    console.log(success);
                }, function (error){
                    console.log("upload error");
                    console.log(error);
                });
        };

        var removeDataSetDir = function (path) {
            dataSetService.removeDataSetDir(path).then (
                function (success) {
                    growl.success(success.data.successMessage , {title: 'Success', ttl: 15000});
                    load();
                }, function (error){
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                });
        };
        //if in dataset browser show current dataset content
        //else show datasets in project
        var load = function () {
            if(currentDS){
                getDir(currentDS);
            }else{
                getAll();
            }
        };
        load();

        self.newDataSetModal = function () {
            ModalService.newDataSet('md', currentDS).then(
                function (success) {
                    growl.success(success.data.successMessage , {title: 'Success', ttl: 15000});
                    load();
                }, function (error) {
                    growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
                });
        };

        self.deleteFile = function (fileName) {
            if(currentDS){
                removeDataSetDir(encodeURIComponent(currentDS + "/" + fileName));
            }else{
                removeDataSetDir(fileName);
            }
        };


      /* Metadata designer */

      self.toggleLeft = buildToggler('left');
      self.toggleRight = buildToggler('right');

      function buildToggler(navID) {
        var debounceFn = $mdUtil.debounce(function () {
          $mdSidenav(navID)
            .toggle()
            .then(function () {
              self.getAllTemplates();
            });
        }, 300);
        return debounceFn;
      };

      self.close = function () {
        $mdSidenav('right').close()
          .then(function () {
            $log.debug("Closed metadata designer");
          });
      };

      self.availableTemplates = [];
      self.newTemplateName = "";
      $scope.extendedFrom = {};

      self.extendedFromBoard = {};

      self.currentTemplateID = "";
      self.currentBoard = {};


      self.getAllTemplates = function () {
        WSComm.send({
          sender: 'evsav',
          type: 'TemplateMessage',
          action: 'fetch_templates',
          message: JSON.stringify({})
        }).then(
          function (data) {
            self.availableTemplates = JSON.parse(data.board).templates;
          }
        );
      };

      self.addNewTemplate = function(){
        return WSComm.send({
          sender: 'evsav',
          type: 'TemplateMessage',
          action: 'add_new_template',
          message: JSON.stringify({templateName: self.newTemplateName})
        }).then(
          function(data){
            self.newTemplateName = "";
            self.getAllTemplates();
            console.log(data);
          }
        );
      };


      self.removeTemplate = function(templateId){
        return WSComm.send({
          sender: 'evsav',
          type: 'TemplateMessage',
          action: 'remove_template',
          message: JSON.stringify({templateId: templateId})
        }).then(
          function(data){
            self.getAllTemplates();
            console.log(data);
          }
        );
      };


      $scope.$watch('extendedFrom', function(newID){
        if (typeof newID === "string"){
          self.selectChanged(newID);
        }
      });

      self.selectChanged = function(extendFromThisID){
        console.log('selectChanged - start: ' + extendFromThisID);
        return WSComm.send({
          sender: 'evsav',
          type: 'TemplateMessage',
          action: 'fetch_template',
          message: JSON.stringify({tempid: parseInt(extendFromThisID)})
        }).then(
          function(success){
            console.log('Fetched data - success.board.column:');
            self.extendedFromBoard = JSON.parse(success.board);
            console.log(self.extendedFromBoard);

            console.log('Fetched data - success:');
            console.log(success);

          }, function(error){
            console.log('Fetched data - error:');
            console.log(error);
          }
        );
      };

      self.extendTemplate = function(){
        return WSComm.send({
          sender: 'evsav',
          type: 'TemplateMessage',
          action: 'add_new_template',
          message: JSON.stringify({templateName: self.newTemplateName})
        }).then(
          function(data){
            var tempTemplates = JSON.parse(data.board);
            var newlyCreatedID = tempTemplates.templates[tempTemplates.numberOfTemplates-1].id;
            console.log('add_new_templatE');
            console.log(data);

            console.log('Sent message: ');
            console.log(self.extendedFromBoard);

            return WSComm.send({
              sender: 'evsav',
              type: 'TemplateMessage',
              action: 'extend_template',
              message: JSON.stringify({tempid: newlyCreatedID, bd: self.extendedFromBoard})
            }).then(
              function(data){
                self.newTemplateName = "";
                self.getAllTemplates();

                console.log('Response from extending template: ');
                console.log(data);
              }
            );
          }
        );
      };


      self.fetchTemplate = function (templateId) {
        self.currentTemplateID = templateId;
        return WSComm.send({
          sender: 'evsav',
          type: 'TemplateMessage',
          action: 'fetch_template',
          message: JSON.stringify({tempid: templateId})
        }).then(
          function(success){
            console.log('fetchTemplate - success');
            self.currentBoard = JSON.parse(success.board);
            console.log(self.currentBoard);
          }, function(error){
            console.log('fetchTemplate - error');
            console.log(JSON.parse(error));
          }
        );
      };


      self.storeTemplate = function () {
        return WSComm.send({
          sender: 'evsav',
          type: 'TemplateMessage',
          action: 'store_template',
          message: JSON.stringify({tempid: self.currentTemplateID, bd: self.currentBoard})
        }).then(
          function(success){
            console.log(success);
          }, function(error){
            console.log(error);
          }
        );
      };

      self.deleteList = function (column) {
        return WSComm.send({
          sender: 'evsav',
          type: 'TablesMessage',
          action: 'delete_table',
          message: JSON.stringify({
            tempid: self.currentTemplateID,
            id: column.id,
            name: column.name,
            forceDelete: column.forceDelete
          })
        }).then(
          function(success){
            console.log(success);
            self.fetchTemplate(self.currentTemplateID)
          }, function(error){
            console.log(error);
          }
        );
      };

      self.storeCard = function (column, card) {
        return WSComm.send({
          sender: 'evsav',
          type: 'FieldsMessage',
          action: 'store_field',
          message: JSON.stringify({
            tempid: self.currentTemplateID,
            tableid: column.id,
            tablename: column.name,
            id: card.id,
            name: card.title,
            type: 'VARCHAR(50)',
            searchable: card.find,
            required: card.required,
            sizefield: card.sizefield,
            description: card.description,
            fieldtypeid: card.fieldtypeid,
            fieldtypeContent: card.fieldtypeContent
          })
        });
      };

      self.addCard = function (column) {
        $scope.currentColumn = column;

        var modalInstance = $modal.open({
            templateUrl: 'views/metadata/newCardModal.html',
            controller: 'NewCardCtrl',
            scope: $scope
         }).result.then(
          function(card){

            console.log('Created card, ready to send:');
            console.log(card);

            self.storeCard(column, card).then(
              function (success) {
                console.log(success);
                self.fetchTemplate(self.currentTemplateID);
              }, function(error){
                console.log(error);
              }
            );

          }, function(error){
            console.log(error);
          }
        );
      };


      self.deleteCard = function (column, card) {
        return WSComm.send({
          sender: 'evsav',
          type: 'FieldsMessage',
          action: 'delete_field',
          message: JSON.stringify({
            tempid: self.currentTemplateID,
            id: card.id,
            tableid: column.id,
            tablename: column.name,
            name: card.title,
            type: 'VARCHAR(50)',
            sizefield: card.sizefield,
            searchable: card.find,
            required: card.required,
            forceDelete: card.forceDelete,
            description: card.description,
            fieldtypeid: card.fieldtypeid,
            fieldtypeContent: card.fieldtypeContent
          })
        }).then(
          function(success){
            console.log(success);
            self.fetchTemplate(self.currentTemplateID);
        }, function(error){
            console.log(error);
        });
      };

      self.addNewList = function () {
        $scope.template = self.currentTemplateID;

        $modal.open({
          templateUrl: 'views/metadata/newListModal.html',
          controller: 'NewListCtrl',
          scope: $scope
        });

      };


      /* TESTING RECEIVE BROADCAST FROM WEBSOCKET SERVICE */
      $rootScope.$on('andreTesting', function (event, data) {
        console.log('BroadcastReceived BOARD:');
        console.log(JSON.parse(data.response.board));
        //self.getAllTemplates();
      });

    }]);

/*
     <DONE>
     fetchTemplates: function () {
     return WSComm.send({
     sender: 'evsav',
     type: 'TemplateMessage',
     action: 'fetch_templates',
     message: JSON.stringify({})
     });
     },
     <DONE>

   <DONE>
     fetchTemplate: function (templateId) {
     return WSComm.send({
     sender: 'evsav',
     type: 'TemplateMessage',
     action: 'fetch_template',
     message: JSON.stringify({tempid: templateId})
     });
     },
   <DONE>

  <DONE>
     storeTemplate: function (templateId, board) {
     return WSComm.send({
     sender: 'evsav',
     type: 'TemplateMessage',
     action: 'store_template',
     message: JSON.stringify({tempid: templateId, bd: board})
     });
     },
  <DONE>

    <DONE>
     extendTemplate: function(templateId, board){
     return WSComm.send({
     sender: 'evsav',
     type: 'TemplateMessage',
     action: 'extend_template',
     message: JSON.stringify({tempid: templateId, bd: board})
     });
     },
      <DONE>

     <DONE>
     addNewTemplate: function(templateName){
     return WSComm.send({
     sender: 'evsav',
     type: 'TemplateMessage',
     action: 'add_new_template',
     message: JSON.stringify({templateName: templateName})
     });
     },
     <DONE>

     <DONE>
     removeTemplate: function(templateId){
     return WSComm.send({
     sender: 'evsav',
     type: 'TemplateMessage',
     action: 'remove_template',
     message: JSON.stringify({templateId: templateId})
     });
     },
     <DONE>

     deleteList: function (templateId, column) {
     return WSComm.send({
     sender: 'evsav',
     type: 'TablesMessage',
     action: 'delete_table',
     message: JSON.stringify({
     tempid: templateId,
     id: column.id,
     name: column.name,
     forceDelete: column.forceDelete
     })
     });
     },

     <DONE>


     <DONE>
     self.storeCard = function (column) {
     return WSComm.send({
     sender: 'evsav',
     type: 'FieldsMessage',
     action: 'store_field',
     message: JSON.stringify({
     tempid: self.currentTemplateID,
     tableid: column.id,
     tablename: column.name,
     id: card.id,
     name: card.title,
     type: 'VARCHAR(50)',
     searchable: card.find,
     required: card.required,
     sizefield: card.sizefield,
     description: card.description,
     fieldtypeid: card.fieldtypeid,
     fieldtypeContent: card.fieldtypeContent
     })
     }).then(
     function(success){
     console.log(success);
     }, function(error){
     console.log(error);
     }
     );
     }
     <DONE>

      <DONE>
     storeCard: function (templateId, column, card) {
     return WSComm.send({
     sender: 'evsav',
     type: 'FieldsMessage',
     action: 'store_field',
     message: JSON.stringify({
     tempid: templateId,
     tableid: column.id,
     tablename: column.name,
     id: card.id,
     name: card.title,
     type: 'VARCHAR(50)',
     searchable: card.find,
     required: card.required,
     sizefield: card.sizefield,
     description: card.description,
     fieldtypeid: card.fieldtypeid,
     fieldtypeContent: card.fieldtypeContent
     })
     });
     },
     <DONE>

     <DONE>
     deleteCard: function (templateId, column, card) {

     return WSComm.send({
     sender: 'evsav',
     type: 'FieldsMessage',
     action: 'delete_field',
     message: JSON.stringify({
     tempid: templateId,
     id: card.id,
     tableid: column.id,
     tablename: column.name,
     name: card.title,
     type: 'VARCHAR(50)',
     sizefield: card.sizefield,
     searchable: card.find,
     required: card.required,
     forceDelete: card.forceDelete,
     description: card.description,
     fieldtypeid: card.fieldtypeid,
     fieldtypeContent: card.fieldtypeContent
     })
     });
     },
     <DONE>


 storeMetadata: function(data){
 return WSComm.send({
 sender: 'evsav',
 type: 'MetadataMessage',
 action: 'store_metadata',
 message: JSON.stringify(data)
 });
 },

 fetchMetadata: function (tableId) {
 return WSComm.send({
 sender: 'evsav',
 type: 'MetadataMessage',
 action: 'fetch_metadata',
 message: JSON.stringify({tableid: tableId})
 });
 },

   <DONE>

   fetchFieldTypes: function(){
       return WSComm.send({
       sender: 'evsav',
       type: 'FieldTypesMessage',
       action: 'fetch_field_types',
       message: 'null'
       });
       }
   <DONE>


 */
