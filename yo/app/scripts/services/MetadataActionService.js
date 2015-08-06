/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .service('MetadataActionService', function (WSComm) {

          return {
            fetchTemplates: function () {
              return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'fetch_templates',
                message: JSON.stringify({})
              });
            },
            addNewTemplate: function (templatename) {
              return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'add_new_template',
                message: JSON.stringify({templateName: templatename})
              });
            },
            removeTemplate: function (templateid) {
              return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'remove_template',
                message: JSON.stringify({templateId: templateid})
              });
            },
            extendTemplate: function (templateId, board) {

              return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'extend_template',
                message: JSON.stringify({tempid: templateId, bd: board})
              });
            },
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
            deleteList: function (templateId, column) {
              return WSComm.send({
                sender: 'evsav',
                type: 'TableMessage',
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
                type: 'FieldMessage',
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
                type: 'FieldMessage',
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
            fetchFieldTypes: function () {
              return WSComm.send({
                sender: 'evsav',
                type: 'FieldTypeMessage',
                action: 'fetch_field_types',
                message: 'null'
              });
            },
            fetchMetadata: function (tableId, inodeId) {
              console.log("fetching metadata for inode " + inodeId);
              return WSComm.send({
                sender: 'evsav',
                type: 'FetchMetadataMessage',
                action: 'fetch_metadata',
                message: JSON.stringify({tableid: tableId, inodeid: inodeId})
              });
            },
            fetchTableMetadata: function(tableId){
              console.log("fetching metadata for table " + tableId);
              return WSComm.send({
                sender: 'evsav',
                type: 'FetchTableMetadataMessage',
                action: 'fetch_table_metadata',
                message: JSON.stringify({tableid: tableId})
              });
            },
            storeMetadata: function (data) {
              return WSComm.send({
                sender: 'evsav',
                type: 'StoreMetadataMessage',
                action: 'store_metadata',
                message: JSON.stringify(data)
              });
            },
            isTableEmpty: function(tableId){
              return WSComm.send({
                sender: 'evsav',
                type: 'CheckTableContentMessage',
                action: 'is_table_empty',
                message: JSON.stringify({tableid: tableId})
              });
            },
            isFieldEmpty: function(fieldId){
              return WSComm.send({
                sender: 'evsav',
                type: 'CheckFieldContentMessage',
                action: 'is_field_empty',
                message: JSON.stringify({fieldid: fieldId})
              });
            },
            updateRawdata: function(rawObj){
              return WSComm.send({
                sender: 'evsav',
                type: 'UpdateMetadataMessage',
                action: 'update_metadata',
                message: JSON.stringify({rawid: rawObj.rawid, rawdata: rawObj.raw})
              });
            }
          };
        });