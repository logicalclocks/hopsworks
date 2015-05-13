/**
 * Created by AMore on 2015-04-24.
 */

'use strict';


angular.module('hopsWorksApp')
  .controller('DatasetsCtrl', ['$scope', '$timeout', '$mdSidenav', '$mdUtil', '$log', '$websocket', 'WSComm',
    function ($scope, $timeout, $mdSidenav, $mdUtil, $log, $websocket, WSComm) {

      var self = this;

      self.datasets = [];
      self.files = [];
      var file = {};

      self.datasets.push('MedicalExperiment');
      self.datasets.push('DNASamples');
      self.datasets.push('TestData');
      self.datasets.push('Movies');
      self.datasets.push('FinanceCalc');
      self.datasets.push('EcoProject');
      self.datasets.push('Measurements');
      self.datasets.push('HugeCollection');


      file = {name: 'Folder', owner: 'André', modified: 'Mar 23', filesize: '4 MB'}
      self.files.push(file);
      file = {name: 'index.html', owner: 'André', modified: 'Mar 29', filesize: '465 KB'}
      self.files.push(file);
      file = {name: 'AndrePage.html', owner: 'André', modified: 'Jan 10', filesize: '33 KB'}
      self.files.push(file);
      file = {name: 'image3.jpg', owner: 'Ermias', modified: 'Apr 7', filesize: '8 MB'}
      self.files.push(file);
      file = {name: 'dump34.sql', owner: 'André', modified: 'Mar 2', filesize: '37 KB'}
      self.files.push(file);
      file = {name: 'Yarn.yml', owner: 'André', modified: 'Feb 25', filesize: '6 MB'}
      self.files.push(file);
      file = {name: 'Yarn2.yml', owner: 'André', modified: 'Apr 3', filesize: '17 MB'}
      self.files.push(file);
      file = {name: 'Yarn3.yml', owner: 'Ermias', modified: 'Jun 2', filesize: '198 KB'}
      self.files.push(file);
      file = {name: 'Yarn4.yml', owner: 'André', modified: 'Oct 23', filesize: '32 MB'}
      self.files.push(file);
      file = {name: 'Yar5.yml', owner: 'Ermias', modified: 'Dec 34', filesize: '2 GB'}
      self.files.push(file);


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

      self.currentTemplate = {};

      self.getAllTemplates = function () {
        WSComm.send({
          sender: 'evsav',
          type: 'TemplateMessage',
          action: 'fetch_templates',
          message: JSON.stringify({})
        }).then(
          function (data) {
            self.availableTemplates = JSON.parse(data.board).templates;
            console.log(self.availableTemplates);
          }
        );
      }

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
      }


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
      }



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

      }


      $scope.$watch('extendedFrom', function(newID){
        if (typeof newID == "string"){
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
          function(data){
            self.extendedFromBoard = data.board.columns;
            console.log(data);
          }, function(error){
            console.log(error);
          }
        )
      }









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

 fetchTemplate: function (templateId) {
 return WSComm.send({
 sender: 'evsav',
 type: 'TemplateMessage',
 action: 'fetch_template',
 message: JSON.stringify({tempid: templateId})
 });
 },

 storeTemplate: function (templateId, board) {
 return WSComm.send({
 sender: 'evsav',
 type: 'TemplateMessage',
 action: 'store_template',
 message: JSON.stringify({tempid: templateId, bd: board})
 });
 },

 extendTemplate: function(templateId, board){
 return WSComm.send({
 sender: 'evsav',
 type: 'TemplateMessage',
 action: 'extend_template',
 message: JSON.stringify({tempid: templateId, bd: board})
 });
 },

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

 fetchFieldTypes: function(){
 return WSComm.send({
 sender: 'evsav',
 type: 'FieldTypesMessage',
 action: 'fetch_field_types',
 message: 'null'
 });
 }


 */

