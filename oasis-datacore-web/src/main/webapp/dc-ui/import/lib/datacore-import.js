  var containerUrl = "http://data.oasis-eu.org/"; // rather auto defined in description.html NOO REQUIRES SwaggerUi
  
  // first four letters of model import file
  function buildModelDomainPrefix(modelFileName) {
     return modelFileName.substring(modelFileName.lastIndexOf('/') + 1, 3).toLowerCase();
  }
  
  function trimIfAnyElseNull(value) {
     if (typeof value === 'string') {
        value = value.trim();
        if (value.length == 0) {
           return null;
        }
        return value;
     }
     return null;
  }
   // on csv value only
   function setTrimIfAny(fields, fieldName, value) {
      if (typeof value !== 'undefined') {
         //if (value.length != 0) {
         value = value.trim();
         var existingValue = fields[fieldName];
         if (value.length != 0 && (typeof existingValue === 'undefined' || existingValue == null)) {
            fields[fieldName] = value;
         }
      }
   }
  
   function identity(s) {
      return s;
   }
   var defaultCsvDateFormat = "YYYYMMDD" + "Z"; // TODO in model csv ?!
   var defaultTimezone = "+01:00"; // fr summer timezone ; TODO winter hour !!
   var convertMap = {
      "date" : function(stringValue) { return moment(stringValue + defaultTimezone,
          defaultCsvDateFormat).format("YYYY-MM-DDTHH:mm:ssZ"); }, // rather than .toISOString()
          // which converts dates to GMT and don't preserve timezone, see http://momentjs.com/docs/
      "resource" : identity, // NOT subresource
      // numbers : see http://stackoverflow.com/questions/5450012/how-to-convert-a-string-to-long-in-javascript
      "boolean" : function(stringValue) {
         return typeof stringValue === 'string' && "true" === stringValue.trim().toLowerCase();
      },
      "int" : function(stringValue) { return parseInt(stringValue, 10); },
      "float" : parseFloat,
      "long" : identity, // javascript has no long, though int ones could be parsed
      "double" : identity, // idem
      "list" : function(stringValue, mixinField) {
         var values = [];
         var stringValues = stringValue.split(",");
         var listElementField = mixinField["dcmf:listElementField"];
         for (var svInd in stringValues) {
            values.push(convertValue(stringValues[svInd], listElementField));
         }
         return values;
      }
      // TODO i18n (list of maps) (not here : map, subresource)
   }
   function convertValue(stringValue, mixinField) {
      var stringValueType = typeof stringValue;
      if (stringValueType === 'undefined' || stringValue == null) {
         return null; // TODO raise error, shouldn't happen in CSV !(?)
      }
      var fieldType = mixinField["dcmf:type"];
      if ("string" === fieldType) {
         if (mixinField["importconf:evalAsJs"]) {
            return eval(stringValue);
         }
         return stringValue;
      }
      // else trim before parsing :
      stringValue = stringValue.trim();
      if (stringValue.length == 0) {
         return null;
      }
      var convertFunction = convertMap[fieldType];
      if (convertFunction === 'undefined') { // TODO error
         return "ERROR unknown type " + fieldType + " for value " + stringValue;
      }
      return convertFunction(stringValue, mixinField);
   }
   
   function setDefaultValueIfAny(resource, mixinField) {
      var defaultStringValue = mixinField["dcmf:defaultStringValue"];
      if (typeof defaultStringValue !== 'undefined') {
         // set default value when not among imported fields (no internalName) :
         if (defaultStringValue.charAt(0) == "\"" && defaultStringValue.charAt(defaultStringValue.length - 1) == "\"") {
            // handling Datacore server-serialized stringValue :
            defaultStringValue = defaultStringValue.substring(1, defaultStringValue.length - 1);
         }
         resource[mixinField["dcmf:name"]] = convertValue(defaultStringValue, mixinField);
         return true;
      }
      return false;
   }
   
   /*function setValueOrDefaultIfAny(resource, mixinField, stringValue) {
      var value = convertValue(stringValue, mixinField);
      if (value != null) {
         resource[mixinField["dcmf:name"]] = value;
         return value;
      }
      var defaultStringValue = mixinField["dcmf:defaultStringValue"];
      if (typeof defaultStringValue !== 'undefined') {
         // set default value when not among imported fields (no internalName) :
         resource[mixinField["dcmf:name"]] = convertValue(defaultStringValue, mixinField);
         return true;
      }
      return false;
   }*/
   
   function contains(a, obj) {
      var i = a.length;
      while (i--) {
         if (a[i] === obj) {
            return true;
         }
      }
      return false;
   }
   
   function findField(fieldName, mixinFields) {
      for (var fInd in mixinFields) {
         var field = mixinFields[fInd];
         if (field["dcmf:name"] === fieldName) {
              return field;
         }
      }
      return null;
   }
   
   function findMixin(mixinName, mixins) {
      for (var mInd in mixins) {
         var mixin = mixins[mInd];
         if (mixin["dcmo:name"] === mixinName) {
            return mixin;
         }
      }
      return null;
   }
   
   function hasMixin(modelOrMixinResourceOrName, mixinName, importState) {
      var modelOrMixinResource = (typeof modelOrMixinResourceOrName === 'string') ?
            findMixin(modelOrMixinResourceOrName, importState.data.involvedMixins) : modelOrMixinResourceOrName;
      return contains(modelOrMixinResource["dcmo:globalMixins"], mixinName);
   }
   
   function mergeValue(existingResource, key, newValue, mixin) {
      if (typeof newValue === 'undefined' || newValue == null
            || typeof newValue === 'string' && newValue.length == 0) {
         return; // no new value to merge
      }
      var existingValue = existingResource[key];
      if (typeof existingValue === 'undefined' || existingValue == null
            || typeof newValue === 'string' && existingValue.length == 0) {
         existingResource[key] = newValue; // TODO more empty, conflicts
         return;
      }
      var field = findField(key, mixin["dcmo:globalFields"]);
      if (field["dcmf:type"] === 'list') {
         var existingList = existingResource[key];
         if (newValue instanceof Array) {
            for (var nvInd in newValue) {
               var newValueElt = newValue[nvInd];
               if (!contains(existingList, newValueElt)) { // TODO better for non primitive values...
                  existingList.push(newValueElt);
               }
            }
         } else if (!contains(existingList, newValue)) { // TODO better for non primitive values...
            existingList.push(newValue);
         }
      } else if (field != null && existingValue === convertValue(field['dcmf:defaultStringValue'], field)) { // (null ex. if @type)
         existingResource[key] = newValue; // allow to override default value
      }
   }

   function convertImportConfValue(stringValue, valueType) {
      var stringValueType = typeof stringValue;
      if (stringValueType === 'undefined' || stringValue == null) {
         return null; // TODO raise error, shouldn't happen in CSV !(?)
      }
      if ("string" === valueType) {
         return stringValue;
      }
      // else trim before parsing :
      stringValue = stringValue.trim();
      if (stringValue.length == 0) {
         return null;
      }
      var convertFunction = convertMap[valueType];
      if (convertFunction === 'undefined') { // TODO error
         return "ERROR unknown type " + valueType + " for value " + stringValue;
      }
      return convertFunction(stringValue);
   }
   function mergeImportConfStringValue(existingResource, key, newStringValue, valueType) {
      var newValue = convertImportConfValue(newStringValue, valueType);
      if (typeof newValue === 'undefined' || newValue == null
            || typeof newValue === 'string' && newValue.length == 0) {
         return; // no new value to merge
      }
      var existingValue = existingResource[key];
      if (typeof existingValue === 'undefined' || existingValue == null
            || typeof newValue === 'string' && existingValue.length == 0) {
         existingResource[key] = newValue; // TODO more empty, conflicts
      }
      // else don't override
   }
   
   function mergeStringValueOrDefaultIfAny(existingResource, key, newStringValue, mixin) {
      var mergeField = findField(key, mixin["dcmo:globalFields"]);
      var newValue = convertValue(newStringValue, mergeField);
      if (newValue === null) {
         var defaultStringValue = mergeField["dcmf:defaultStringValue"];
         if (typeof defaultStringValue !== 'undefined') {
            newValue = convertValue(defaultStringValue, mergeField);
         }
      }
      mergeValue(existingResource, key, newValue, mixin);
   }
   

   function decodeIdSaveIfNot(idValue, idField) {
      if (typeof idField !== 'undefined' && idField !== null) {
         var dontDecode = idField["importconf:dontEncodeIdInUri"];
         if (dontDecode) {
            return idValue;
         }
      }
      return decodeURI(idValue); // decoding !
      // (rather than decodeURIComponent which would not accept unencoded slashes)
   }
   function encodeIdSaveIfNot(idValue, idField) {
      if (typeof idField !== 'undefined' && idField !== null) {
         var dontEncode = idField["importconf:dontEncodeIdInUri"];
         if (dontEncode) {
            return idValue;
         }
      }
      return encodeURI(idValue); // encoding !
      // (rather than encodeURIComponent which would not accept unencoded slashes)
   }
   
   

   function importedResourcePosted(resourceOrData, importStateRes, importState, kind, counter, success, error) {
      importStateRes.postedNb++;
      if (typeof resourceOrData === 'object' && typeof resourceOrData["_headers"] === 'object') {
         importStateRes.errors.push([ resourceOrData._body._body, resourceOrData.request._body ]);
         var requestBodyHtml;
         try {
            requestBodyHtml = toolifyDcListOrResource(eval(resourceOrData.request._body._data));
            // NB. no need of wrapping by [...] and taking [0] because always there
         } catch (ex) {
            requestBodyHtml = resourceOrData.request._body;
         }
         importStateRes.errorHtml += "<p>-&nbsp;" + resourceOrData._body._body
               + " :<br/>" + requestBodyHtml + "<p/>";
      }
      if (true/*importStateRes.postedNb % 1000 == 0 || importStateRes.postedNb > importStateRes.toBePostedNb - 10*/) {
         var msg = "Posted <a href=\"#importedResourcesFromCsv\">" + importStateRes.postedNb
               + " " + kind + "s</a> (";
         if (importStateRes.errors.length === 0) {
            msg += "no error";
            msg += "), <a href=\"#datacoreResources\">browse them</a>";
         } else {
            msg += "<a href=\"#datacoreResources\">" + importStateRes.errors.length + " error"
            if (importStateRes.errors.length !== 1) {
               msg += "s";
            }
            msg += "</a>";
            msg += ")";
         }
         counter.html(msg);
         if (importStateRes.errors.length != 0) {
            $('.mydata').html(importStateRes.errorHtml);
         }
      }
      if (importStateRes.postedNb === importStateRes.toBePostedNb) {
         if (importStateRes.errors.length == 0) {
            console.log("INFO Successfully posted " + importStateRes.postedNb + " " + kind + "s.");
            if (typeof success !== 'undefined') {
               success(importState);
            }
         } else {
            console.log("WARNING Posted " + importStateRes.postedNb + " "+ kind
                  + "s with " + importStateRes.errors.length + " errors.");
            if (typeof error !== 'undefined') {
               error(importState);
            }
         }
      }
   }
   
   // pathInFieldNameTree for log only
   function csvRowToDataResource(mixin, resourceRow,
         fieldNameTree, pathInFieldNameTree,
         modelTypeToRowResources, importState) {
      var typeName = mixin["dcmo:name"];
      var topLevelResource = false;
      if (fieldNameTree === null) {
         topLevelResource = true;
         fieldNameTree = importState.model.mixinNameToFieldNameTree[typeName];
      }
      var resources = importState.data["resources"];
      var resource;
      if (modelTypeToRowResources != null) {
            resource = modelTypeToRowResources[typeName];
         if (typeof resource === 'undefined') {
            resource = {};
         }
      } else {
         resource = {};
      }
      var mixinHasValue = false;
      var mixinHasAutolinkedValue = false;
      var mixinHasDefaultValue = false;
      var mixinFields = mixin["dcmo:globalFields"];
      var enrichedModelOrMixinFieldMap = importState.model.modelOrMixins[typeName]["dcmo:globalFields"]; // for import-specific data lookup

      for (var fieldName in enrichedModelOrMixinFieldMap) {
         var mixinField = enrichedModelOrMixinFieldMap[fieldName];
      /*for (var fInd in mixinFields) {
         var mixinField = mixinFields[fInd];
         var fieldName = mixinField["dcmf:name"];*/
         if (typeof resource[fieldName] !== 'undefined') {
            mixinHasValue = true;
            continue; // skipping, value already found in this row
         }
         
         var subFieldNameTree = fieldNameTree[fieldName];
         /*var mixinField = findField(fieldName, mixinFields);*/
         var enrichedModelOrMixinField = enrichedModelOrMixinFieldMap[fieldName]; // allows lookup of import-only conf
         // TODO if map (??) or (sub!)resource, recurse !
           
         /*if (typeName === "elec:Canton_0" && fieldName === "elec_Canton:ID") {
              console.log("canton");
         }*/
         var fieldHasValue = false;
         var resourceType = mixinField["dcmf:resourceType"];
         
         if (subFieldNameTree !== null && typeof subFieldNameTree === 'object') { // ex. {} ; typeof mixinField["dcmf:type"] !== 'resource'
            // resource tree : looking for value in an (indirectly) referenced resource
            //if (mixinField["dcmf:type"] === "resource") {
            var resourceMixin = findMixin(resourceType, importState.data["involvedMixins"]);
            if (typeof resourceMixin === 'undefined') {
               importState.data.errors.push({ errorType : "unknownReferencedMixin", referencedMixin : resourceType,
                     message : "ERROR can't find resource field referenced mixin " +
                     + resourceType + " among involved mixins" });
            } else {
               // (sub)resource field values might be provided, let's look for them
               var subresource = csvRowToDataResource(resourceMixin, resourceRow,
                     subFieldNameTree, pathInFieldNameTree + "." + fieldName, null, importState);
               if (true) { // resource ; TODO TODO better is subresource ; typeof value !== 'undefined' && value != null && value.length != 0
                  resource[fieldName] = subresource["@id"]; // uri
               } else { // subresource
                  resource[fieldName] = subresource;
               }
               fieldHasValue = true;
            }
            
         } else if (!fieldHasValue) {
            // looking for local value :
            // (including if (non sub)resource uri)
            // in column mixinField["importconf:internalName"] or else fieldName
            
            var value;
            var fieldInternalName = (typeof subFieldNameTree === 'undefined') ? null : subFieldNameTree;
                  ///enrichedModelOrMixinField["importconf:internalName"]; // NB. this import-specific conf has been enriched in refreshed involvedMixins' global fields
            var unknownFieldInternalName = fieldInternalName === null;
            if (!unknownFieldInternalName || topLevelResource) {
               if (!unknownFieldInternalName) {
                  value = resourceRow[fieldInternalName];
                  if (typeof value === 'undefined') { // look in fieldName column as failback
                     value = resourceRow[fieldName];
                  }
               } else { // if (topLevelResource) { // look in fieldName column
                  value = resourceRow[fieldName];
               }
               if (typeof value !== 'undefined') {
                  value = convertValue(value, mixinField);
                  resource[fieldName] = value;
                  if (value == null || typeof value === 'string' && value.length == 0) { // TODO better empty
                     // set default value when among imported fields but empty :
                     setDefaultValueIfAny(resource, mixinField);
                     mixinHasDefaultValue = true;
                  } else {
                     fieldHasValue = true;
                  }
                  // else allow to import null value
               } else if (!unknownFieldInternalName) {
                  importState.data.warnings.push({ warningType : "noColumnForFieldInternalName",
                        mixin : typeName, field : fieldName, fieldInternalName : fieldInternalName,
                        errorMessage : "Can't find column '" + fieldInternalName
                        + "' (field internal name) nor '" + fieldName
                        + "' for field " + fieldName + " in mixin " + typeName });
               }
            } // else not top level, so has to use an internal field name other than fieldName, so skipping import
         }
         
         if (!fieldHasValue) {
            // trying auto linking :
            if (typeof resourceType !== 'undefined' && modelTypeToRowResources != null
                  && resourceType !== typeName) { // itself (case of ex. node.parentNode)
               var rowResource = modelTypeToRowResources[resourceType];
               if (typeof rowResource !== 'undefined') {
                  var uri = rowResource["@id"];
                  if (typeof uri !== 'undefined') { // avoiding incomplete rowResource
                     if (true) { // resource ; TODO TODO better is subresource ; typeof value !== 'undefined' && value != null && value.length != 0
                        value = uri;
                     } else { // subresource
                        value = rowResource;
                     }
                     resource[fieldName] = value;
                     fieldHasValue = typeof value !== 'undefined';
                     mixinHasAutolinkedValue = mixinHasAutolinkedValue || fieldHasValue;
                  } // else rowResource id may still be incomplete
               } // else rowResource not yet parsed
            }
         }
         
         if (!fieldHasValue) {
            // set default value when not among imported fields (no internalName) :
            mixinHasDefaultValue = setDefaultValueIfAny(resource, mixinField) || mixinHasDefaultValue;
         } else {
            mixinHasValue = true;
         }
      }
      
      if (typeof resource["@id"] === 'undefined') { // NB. autolinking not enough to build id
         // create or update resource :
           
         if (modelTypeToRowResources != null) {
            modelTypeToRowResources[typeName] = resource; // in case not yet there
         }
           
         // build id :
         var id = null; // NB. id is encoded as URIs should be, BUT must be decoded before used as GET URI
         // because swagger.js re-encodes (per path element because __unencoded__-prefixed per hack)
         // out of fields
         var indexToEncodedValue = {};
         for (var idFieldName in enrichedModelOrMixinFieldMap) {
            var idField = enrichedModelOrMixinFieldMap[idFieldName];
            var indexInId = idField["dcmf:indexInId"]; // NB. this import-specific conf has been enriched in refreshed involvedMixins' global fields
            if (typeof indexInId === 'number') {
               ///var idFieldName = idField["dcmf:name"];
               var idValue = resource[idFieldName];
               if (typeof idValue === 'undefined' || idValue === null || idValue === "") {
                  //console.log("Missing value for id field " + idFieldName
                   //      + ", clearing others (" + Object.keys(indexToEncodedValue).length
                   //      + ") ; below " + pathInFieldNameTree + " in :");
                  //console.log(resource);
                  if (!(mixinHasValue || mixinHasDefaultValue)) {
                     return null; // nothing to import, skip resource
                  }
                  return resource; // abort resource creation in this loop (don't add it to uri'd resources yet)
               }
               if (typeof idField["dcmf:resourceType"] !== 'undefined') { // getting ref'd resource id
                  var uri;
                  if (typeof idValue === 'string') { // resource
                     uri = idValue;
                  } else { // subresource
                     uri = idValue["@id"];
                     if (typeof uri === 'undefined') {
                        //console.log("Missing uri for resource id field " + idFieldName
                        //      + ", clearing others (" + Object.keys(indexToEncodedValue).length
                        //      + ") ; below " + pathInFieldNameTree + " in :");
                        console.log(resource);
                        return resource; // without adding it to uri'd resources
                     }
                  }
                  var iri = uri.replace(/^.*\/\/*dc\/type\/*/, ""); //uri.substring(uri.indexOf("/dc/type/") + 9);
                  var idEncodedValue = iri.substring(iri.indexOf("/") + 1);
                  indexToEncodedValue[indexInId] = {v : idEncodedValue, uri : uri };
               } else {
                  indexToEncodedValue[indexInId] = {v : encodeIdSaveIfNot(idValue, idField) };
               }
            }
         }
         var ancestors = null;
         if (typeof mixin["dcmo:collectionName"] !== "undefined" // only if model
               && contains(mixin["dcmo:globalMixins"], "o:Ancestor_0")) {
            ancestors = [];
         }
         for (var idevInd in indexToEncodedValue) {
            var idValueElt = indexToEncodedValue[idevInd];
            if (id === null) {
               id = idValueElt.v;
            } else {
               id += "/" + idValueElt.v;
            }
            
            // also computing ancestors :
            if (ancestors !== null && typeof idValueElt.uri !== 'undefined') {
               var ancestor = resources[idValueElt.uri];
               // TODO check if ancestor not known (ex. string resource reference...)
               if (typeof ancestor === 'undefined') {
                  ancestors = null;
                  importState.data.errors.push({ errorType : "cantFindAncestorAmongParsedResources",
                        resource : resource, ancestorUri : idValueElt.uri });
               } else if (!hasMixin(ancestor["@type"][0], "o:Ancestor_0", importState)) {
                  ancestors = null;
                  importState.data.errors.push({ errorType : "ancestorHasNotAncestorMixin",
                     resource : resource, ancestor : ancestor });
               } else {
                  var ancestorAncestors = ancestor["o:ancestors"];
                  if (typeof ancestorAncestors === 'undefined') {
                     ancestors = null;
                     importState.data.errors.push({ errorType : "ancestorHasNotDefinedAncestors",
                        resource : resource, ancestor : ancestor });
                  }
                  for (var aInd in ancestorAncestors) {
                     ancestors.push(ancestorAncestors[aInd]);
                  }
               }
            }
         }
         
         if (id === null) { // scripted id
            var idGenJs = mixin["dcmo:idGenJs"];
            if (typeof idGenJs === 'string' && idGenJs.length != 0) {
               var r = resource;
               id = encodeURI(decodeURI(eval(idGenJs))); // in case idGenJs forgot do encode (per path element)
            }
         }
         
         if (id === null) { // CUSTOM
            importState.data.warnings.push({ warningType : "noConfForId",
                  mixin : typeName, row : resourceRow,
                  message : "No conf for id, using custom gen - in mixin "
                  + typeName + " and resourceRow " + resourceRow });
            // sample :
            /*if (typeName === 'elec:City_0') {
               id = encodeURI(resourceRow["inseeville"]) + '/' + encodeURI(resourceRow["numero_electeur"]); // TODO TODO !!! in second pass ??
            }*/
         }
         
         var uri = containerUrl + "dc/type/" + typeName + "/" + id;
         // NB. uri is encoded as URIs should be, BUT must be decoded before used as GET URI
         // because swagger.js re-encodes (per path element because __unencoded__-prefixed per hack)
         var existingResource = resources[uri];
         // TODO TODO subresource case
         if (typeof existingResource === 'undefined') {
            resource["@id"] = uri;
            //resource["o:version"] = -1;
            resource["@type"] = [ typeName ];
            if (ancestors !== null) {
               ancestors.push(uri); // itself
               resource["o:ancestors"] = ancestors;
            }
            // adding it :
            resources[uri] = resource;
            return resource;
         } else {
            // merge (this mixin's found values) over existing resource :
            for (var key in resource) { // works for fields but also native fields ex. modified...
               mergeValue(existingResource, key, resource[key], mixin); // merge new value in existing resource
            }
            if (modelTypeToRowResources != null) {
               modelTypeToRowResources[typeName] = existingResource;
            }
            return existingResource;
         }
      }
      return resource;
   }
           
   function csvToData(resultsData, importState) {
      var internalFieldNames = importState.data.internalFieldNames;
      var involvedMixins = importState.data.involvedMixins;
      var resources = importState.data.resources;
      for (var rInd in resultsData) {
        console.log("row " + rInd);//
        ///var importStateRowData = {};
        ///importState.data.push(importStateRowData);
        // TODO by push / step
        var resourceRow = resultsData[rInd];
        var modelTypeToRowResources = {};
        
        // mixins loop :
        var missingIdFieldResourceOrMixins;
        var previousMissingIdFieldResourceOrMixinNb = -1;
        var loopIndex = 0;
        do {
           missingIdFieldResourceOrMixins = []; // NB. fastest way to empty an array
           //console.log("row mixins loop " + loopIndex + " " + missingIdFieldResourceOrMixins);//
        
        for (var mInd in involvedMixins) {
             // for each mixin, find its values in the current row :
             var mixin = involvedMixins[mInd];
             
             var importedResource = csvRowToDataResource(mixin, resourceRow, null,
                   mixin["dcmo:name"], modelTypeToRowResources, importState);
             
             if (importedResource == null) {
                // nothing to import (or only autolinked values), skip resource
             } else if (typeof importedResource["@id"] === 'undefined') {
                //console.log("gzzk uri should be undefined : " + importedResource["@id"]);//
                //console.log(JSON.stringify(importedResource, null, null));//
                missingIdFieldResourceOrMixins.push(importedResource);
             }
        }

           if (missingIdFieldResourceOrMixins.length // size equality enough if involvedMixins don't change
                      == previousMissingIdFieldResourceOrMixinNb || loopIndex > 20) {
              importState.data.errors.push({ errorType : "missingIdFields",
                 resources : missingIdFieldResourceOrMixins, row : resourceRow });
              console.log("csvToData loop aborted, still missing id fields in resources :\n"
                    + JSON.stringify(missingIdFieldResourceOrMixins, null, "\t")
                    + "\n   in resourceRow :\n" + JSON.stringify(resourceRow, null, null));
              //console.log("   with resources " + JSON.stringify(resources, null, null));
              break;
           }
           previousMissingIdFieldResourceOrMixinNb = missingIdFieldResourceOrMixins.length;
           loopIndex++;
        
        } while (missingIdFieldResourceOrMixins.length != 0);

        ///importStateRowData["loops"] = loopIndex + 1;
        ////importStateRowData["errors"] = errors;
        
        if (rInd % 1000 == 0) {
           $('.resourceRowCounter').html("Handled <a href=\"#importedJsonFromCsv\">" + rInd + " rows</a>");
        }
     }

      $('.resourceRowCounter').html("Handled <a href=\"#importedJsonFromCsv\">" + rInd
            + " rows</a> (<a href=\"#datacoreResources\">" + importState.data.errors.length + " errors</a>)");
      if (importState.data.errors.length != 0) {
         //$('.mydata').html(
         $('.importedResourcesFromCsv').html("<b>Errors :</b><br/>"
               + JSON.stringify(importState.data.errors, null, '\t').replace(/\n/g, '<br>')
               + "<br/><br/><b>Warnings :</b><br/>"
               + JSON.stringify(importState.data.warnings, null, '\t').replace(/\n/g, '<br>'));
         
      } else {
         // display full resources only if no errors to display :
         //var resourcesPrettyJson = JSON.stringify(resources, null, '\t').replace(/\n/g, '<br>');
         var resourcesPrettyJson = toolifyDcResource(resources, 0);
         $('.importedResourcesFromCsv').html(resourcesPrettyJson);
      }
      
      function importedDataPosted(resourceOrData) {
         importedResourcePosted(resourceOrData, importState.data, importState,
               "resource", $('.resourceCounter'));
      }
      importState.data["toBePostedNb"] = importState.data.resources.length;
      for (var uri in resources) {
         // TODO mass version update !
         var relativeUrl = uri.substring(uri.indexOf("/dc/type/"));
         getData(relativeUrl, function (returnedResource) {
            // updating existing resource : 
            // NB. can't access original "resource" variable because has been changed since call is async
            var upToDateResourceUri = returnedResource["@id"];
            var upToDateResource = resources[upToDateResourceUri];
            upToDateResource["o:version"] = returnedResource["o:version"];
            var resourceIri = upToDateResourceUri.substring(upToDateResourceUri.indexOf("/dc/type/") + "/dc/type/".length);
            var modelType = decodeURIComponent(resourceIri.substring(0, resourceIri.indexOf("/")));
            //var resourceId = decodeURIComponent(resourceIri.substring(resourceIri.indexOf("/") + 1));
            postAllDataInType(modelType, JSON.stringify([ upToDateResource ], null, null),
                  importedDataPosted, importedDataPosted);
         }, function (data) {
            // creating new resource :
            var resourceIri = data.request.path.replace(/^\/*dc\/type\/*/, "");
            var modelType = decodeURIComponent(resourceIri.substring(0, resourceIri.indexOf("/")));
            var resourceId = decodeURIComponent(resourceIri.substring(resourceIri.indexOf("/") + 1));
            var upToDateResourceUri = containerUrl + "dc/type/" + modelType + "/" + resourceId;
            var upToDateResource = resources[upToDateResourceUri];
            postAllDataInType(modelType, JSON.stringify([ upToDateResource ], null, null),
                  importedDataPosted, importedDataPosted);
         });
      }
  }
  
   function getResourceRowLimit() {
      var resourceRowLimit = parseInt($(".resourceRowLimit").val(), 10);
      if (typeof resourceRowLimit === 'number') { // && resourceRowLimit < 500
         return resourceRowLimit;
      }
      return 50;
   }
   function fillData(importState) {
      $('.resourceRowCounter').html("");
      $('.resourceCounter').html("");
      //console.log("fillData");//
      var resourceParsingConf = {
            download: true,
            header: true,
            preview: getResourceRowLimit(), // TODO !!
            complete: function(results) {
               console.log("Remote file parsed!", results);
               ///var results = eval('[' + data.content.data + ']')[0];
               var prettyJson = JSON.stringify(results.data, null, '\t').replace(/\n/g, '<br>');
               $('.importedJsonFromCsv').html(prettyJson);
               // TODO handle errors...

               var involvedMixins = importState.data.involvedMixins;
               
               var dataColumnNames = results.meta.fields;
               importState.data["dataColumnNames"] = dataColumnNames;
               
               // imported field names
               // adding field names with internal field name :
               var importedFieldNames = [];
               var fieldNamesWithInternalNameMap = importState.model.fieldNamesWithInternalNameMap;
               for (var fnInd in fieldNamesWithInternalNameMap) {
                  importedFieldNames.push(fnInd);
               }
               // adding prefixed data column names :
               for (var dcnInd in dataColumnNames) {
                  var dataColumnName = dataColumnNames[dcnInd];
                  if (dataColumnName.indexOf(':') !== -1 && fieldNamesWithInternalNameMap[dataColumnName] === null) {
                     importedFieldNames.push(dataColumnName);
                  }
               }
               importState.data["importedFieldNames"] = importedFieldNames;
               
      	      findDataByType("/dc/type/dcmo:model_0?dcmo:fields.dcmf:name=$in"
                     + JSON.stringify(importState.data.importedFieldNames, null, null), function(fieldNameMixinsFound) {
                     for (var fnmInd in fieldNameMixinsFound) {
                        involvedMixins.push(fieldNameMixinsFound[fnmInd]);
                     }

                     // enriching modelOrMixins shortcuts with import-specific info ex. internalName at global level :
                     // TODO LATER also local
                     for (var imInd in involvedMixins) {
                        var involvedMixin = involvedMixins[imInd];
                        var modelOrMixin = importState.model.modelOrMixins[involvedMixin["dcmo:name"]];
                        // global mixins :
                        modelOrMixin["dcmo:globalMixins"] = involvedMixin["dcmo:globalMixins"];
                        // global fields :
                        var involvedMixinFieldArray = involvedMixin["dcmo:globalFields"];
                        var modelOrMixinLocalFieldMap = modelOrMixin["dcmo:fields"];
                        var enrichedModelOrMixinFieldMap = {};
                        importState.model.modelOrMixins[involvedMixin["dcmo:name"]]["dcmo:globalFields"] = enrichedModelOrMixinFieldMap;
                        for (var fInd in involvedMixinFieldArray) {
                           var involvedMixinField = involvedMixinFieldArray[fInd];
                           var fieldName = involvedMixinField["dcmf:name"];
                           var enrichedModelOrMixinField = modelOrMixinLocalFieldMap[fieldName];
                           if (typeof enrichedModelOrMixinField !== 'undefined') {
                              // let's enrich the refreshed involvedMixin with import data it doesn't know about :
                              for (var fieldKey in enrichedModelOrMixinField) {
                                 if (typeof involvedMixinField[fieldKey] === 'undefined') { // ex. "importconf:internalName"
                                    involvedMixinField[fieldKey] = enrichedModelOrMixinField[fieldKey];
                                 }
                              }
                           } // else ex. field not local
                           // let's add it as global fields in the modelOrMixin shortcuts :
                           enrichedModelOrMixinFieldMap[fieldName] = involvedMixinField;
                        }
                     }
                     
                     csvToData(results.data, importState);
               });
            }
      }
      if ($(".resourceFile").val() != "") {
         $(".resourceFile").parse({ config : resourceParsingConf });
      } else {
         Papa.parse("samples/openelec/electeur_v26010_sample.csv?reload="
               + new Date().getTime(), resourceParsingConf); // to prevent browser caching
      }
   }
  
   function importField(fieldRow, fieldName, fieldNameTree, mixin, mixinTypeName, importState) {
      var field = mixin["dcmo:fields"][fieldName];
      if (typeof field === 'undefined') {
         var fieldUri = containerUrl + "dc/type/dcmf:field_0/"
               + mixinTypeName + "/" + fieldName; // TODO sub sub resource
         var fieldDataType = fieldRow["Data type"];
         var fieldType = importState.typeMap[fieldDataType.toLowerCase()];
         if (typeof fieldType === 'undefined') {
            fieldType = "resource";
         }
         var fieldOrListField = {
               "dcmf:name" : fieldName, // TODO prefix, case
               "@id" : fieldUri,
               "o:version" : 0, // TODO top level's ?
               "@type" : [ "dcmf:field_0" ], // TODO version, default one ?
               "dcmf:type" : fieldType, // TODO map
         }; // TODO prefixes, case, version
         
         if (fieldType === "resource") {
            fieldOrListField["dcmf:resourceType"] = buildMixinName(fieldDataType, importState); // TODO case
         }
         if (convertMap["boolean"](fieldRow["Multiple"])) {
            field = {
                  "dcmf:name" : fieldName, // TODO prefix, case
                  "@id" : fieldUri,
                  "o:version" : 0, // TODO top level's ?
                  "@type" : [ "dcmf:field_0" ], // TODO version, default one ?
                  "dcmf:type" : "list",
                  "dcmf:listElementField" : fieldOrListField
            }; // TODO prefixes, case, version
         } else {
            field = fieldOrListField;
         }
         // adding it :
         mixin["dcmo:fields"][fieldName] = field;
         // TODO trim...
         // TODO checks...
         // TODO also format ex. date
      } // else TODO (LATER) conflicts & merge...

      // more init :
      if (field["dcmf:type"] === "list") {
         var listElementField = field["dcmf:listElementField"];
         mergeStringValueOrDefaultIfAny(listElementField, "dcmf:required", fieldRow["required"],
               importState.metamodel["dcmf:field_0"]); // meaningless for list but required by ModelResourceMappingService for now
         mergeStringValueOrDefaultIfAny(listElementField, "dcmf:queryLimit", fieldRow["queryLimit"], importState.metamodel["dcmf:field_0"]);
      }
      
      mergeStringValueOrDefaultIfAny(field, "dcmf:required", fieldRow["required"], importState.metamodel["dcmf:field_0"]);
      mergeStringValueOrDefaultIfAny(field, "dcmf:queryLimit", fieldRow["queryLimit"],
            importState.metamodel["dcmf:field_0"]); // meaningless for list but required by ModelResourceMappingService for now
      mergeStringValueOrDefaultIfAny(field, "dcmf:isInMixinRef", fieldRow["isInMixinRef"],
            importState.metamodel["dcmf:field_0"]); // meaningless for list but required by ModelResourceMappingService for now
      mergeStringValueOrDefaultIfAny(field, "dcmf:indexInId", fieldRow["indexInId"], importState.metamodel["dcmf:field_0"]);
      mergeStringValueOrDefaultIfAny(field, "dcmf:defaultStringValue", fieldRow["defaultValue"], importState.metamodel["dcmf:field_0"]);
      
      // import conf-specific (not in server-side model) :
      /*mergeImportConfStringValue(field, "importconf:internalName",
            fieldRow["Internal field name"].trim(), // else can't find ex. OpenElec "code_departement_naissance " !!
            "string");*/
      mergeImportConfStringValue(field, "importconf:dontEncodeIdInUri", fieldRow["dontEncodeIdInUri"], "boolean");
      mergeImportConfStringValue(field, "importconf:evalAsJs", fieldRow["jsToEval"], "string");

      // import plan-specific (not in server-side model nor generic field import conf) :
      if (typeof fieldNameTree[fieldName] !== 'object') { // ex. 'undefined' or {} (should not happen ?)
         ///fieldNameTreeCur[fieldName] = null; // NB. used as hashset, field is gotten from mixin instead
         mergeImportConfStringValue(fieldNameTree, fieldName,
               fieldRow["Internal field name"].trim(), // else can't find ex. OpenElec "code_departement_naissance " !!
               "string");
         if (fieldNameTree[fieldName] !== null) {
            importState.model.fieldNamesWithInternalNameMap[fieldName] = null; // used as a set
         }
      } // else may have already been seen within fieldPath
      
      // for app.js / openelec (NOT required) :
      var description = trimIfAnyElseNull(fieldRow["Description"]);
      var precision = trimIfAnyElseNull(fieldRow["Precision"]);
      if (description != null || precision != null) {
         var doc = "";
         if (description != null) {
            doc += description;
            if (precision != null) {
               doc += " " + precision;
            }
         } else {
            doc += precision;
         }
         var existingDoc = field["dcmf:documentation"];
         if (typeof existingDoc === 'undefined' || existingDoc == null) {
            field["dcmf:documentation"] = doc;
         }
      }
      /*if (!field["dcmf:isInMixinRef"]) {
         field["dcmf:isInMixinRef"] = fieldRow["isInMixinRef"] === "true" // TODO : compute ??
      }
      var indexInIdString = trimIfAnyElseNull(fieldRow["indexInId"]);
      if (indexInIdString !== null) {
         if (typeof field["dcmf:indexInId"] === 'undefined' || field["dcmf:indexInId"] == null) {
            field["dcmf:indexInId"] = new Number(indexInIdString); // TODO : compute
         }
      }
      setTrimIfAny(field, "dcmf:defaultStringValue", fieldRow["defaultValue"]); // TODO for other apps...
      setTrimIfAny(field, "importconf:internalName", fieldRow["Internal field name"]); // TODO for other apps...
      // TODO if 'undefined' throw bad import conf
      */
      return field;
   }
   
   function buildMixinName(mixinName, importState) {
      if (typeof mixinName === 'undefined' || mixinName === null || mixinName.length === 0) {
         return null; // skipping empty line
      }
      // TODO case
      if (mixinName.indexOf(":") == -1) {
         mixinName = importState.domainPrefix + ":" + mixinName; // NO . else KO in query (and tree path parsing)
      }
      if (mixinName.indexOf("_") == -1) {
          mixinName = mixinName + "_" + importState.mixinMajorVersion; // NO . else KO in query (and tree path parsing)
      }
      return mixinName;
   }
   
   // callback(mixins, mixinNameToFieldNameTree)
   function csvToModel(resultsData, importState, callback) {
      var mixinNameToFieldNameTree = importState.model.mixinNameToFieldNameTree;
      var mixins = importState.model.modelOrMixins;
        
      var errors;
      var loopMaxIndex = 20;
      var loopIndex = -1;
      do {
         errors = [];
         if (loopIndex > loopMaxIndex) {
            break;
         }
         loopIndex++;
        
        
        // import plan (& internalName) :
        fieldsLoop : for (var fInd in resultsData) {
           var fieldRow = resultsData[fInd];
           var mixinName = buildMixinName(fieldRow["Mixin"], importState); // TODO case
           if (mixinName === null) {
              continue; // skipping empty line
           }

           var fieldPath = fieldRow["Field name"];
           var fieldPathElements = fieldPath.split("."); // at least one
           
           var mixin = mixins[mixinName];
           var isModel = !convertMap["boolean"](fieldRow["Is Mixin"]);
           var mixinTypeName = ((isModel) ? "dcmo:model_0" : "dcmi:mixin_0") + "/" + mixinName;
           if (typeof mixin == 'undefined') {
              mixin = {
                 "dcmo:name" : mixinName,
                 "@id" : containerUrl + "dc/type/" + mixinTypeName,
                 // more init :
                 "dcmo:majorVersion" : importState.mixinMajorVersion,
                 "dcmo:fields" : {},
              };
              mixins[mixinName] = mixin;
            } else {
               var hasBeenModel = typeof mixin["dcmo:collectionName"] !== 'undefined';
               if (isModel && !hasBeenModel) {
                  mergeStringValueOrDefaultIfAny(mixin, "dcmo:collectionName", mixinName, importState.metamodel["dcmo:model_0"]); // marks it as model
                  // uris of mixin & fields are false and must be reset and recomputed in another loop :
                  errors.push({ errorType : "modelWasThoughtToBeMixinAndMustBeReparsed",
                        "mixin" : mixinName }); // setting up another loop on fields
                  loopMaxIndex++; // allowing the loop no matter what
                  mixinTypeName = "dcmo:model_0/" + mixinName;
                  mixin["@id"] = containerUrl + "dc/type/" + mixinTypeName;
                  mixin["dcmo:fields"] = {};
               } else {
                  isModel = isModel || hasBeenModel;
               }
            }
           mergeStringValueOrDefaultIfAny(mixin, "dcmo:documentation", fieldRow["documentation"], importState.metamodel["dcmi:mixin_0"]); // TODO required
           mergeStringValueOrDefaultIfAny(mixin, "dcmo:mixins", fieldRow["Has Mixins"], importState.metamodel["dcmi:mixin_0"]);
           mergeStringValueOrDefaultIfAny(mixin, "dcmo:fieldAndMixins", fieldRow["fieldAndMixins"], importState.metamodel["dcmi:mixin_0"]);
           // TODO any other DCMixin field
           if (isModel) {
              // for models only :
              mergeStringValueOrDefaultIfAny(mixin, "dcmo:maxScan", fieldRow["maxScan"], importState.metamodel["dcmo:model_0"]);
              mergeStringValueOrDefaultIfAny(mixin, "dcmo:isHistorizable", fieldRow["isHistorizable"], importState.metamodel["dcmo:model_0"]);
              mergeStringValueOrDefaultIfAny(mixin, "dcmo:isContributable", fieldRow["isContributable"], importState.metamodel["dcmo:model_0"]);
              // TODO any other DCModel field
           }

           // filling import plan :
           var rootMixinName = mixinName;
           var fieldNameTreeCur = mixinNameToFieldNameTree[rootMixinName];
           if (typeof fieldNameTreeCur === 'undefined') {
              fieldNameTreeCur = {};
              mixinNameToFieldNameTree[rootMixinName] = fieldNameTreeCur;
           }
           for (var i = 0; i < fieldPathElements.length - 1 ; i++) {
              var fieldName = fieldPathElements[i];
              if (fieldName.indexOf(":") == -1) {
                 var fieldMixinPrefix = mixin["dcmo:name"].replace(":", "_").replace("_" + importState.mixinMajorVersion, ""); // TODO better domainPrefix + "_" + mixinShortName; // NO . else KO in query (and tree path parsing)
                 fieldName = fieldMixinPrefix + ":" + fieldName; // buildFieldName()
              }
              // TODO fieldName from this short name
              // following the link in the model :
              var subField = mixin["dcmo:fields"][fieldName];
              if (typeof subField === 'undefined') {
                 // TODO error field must be defined in first pass NOO TODO TODO TODO TODO
                 errors.push({ errorType : "missingSubFields", mixin : rootMixinName, field : fieldPath,
                       intermediateMixin : mixin, subfield : fieldName }); // setting up another loop on fields
                 continue fieldsLoop;
              }
              var subFieldResourceType = subField["dcmf:resourceType"];
              mixin = mixins[subFieldResourceType];
              if (typeof mixin === 'undefined') {
                 // TODO error mixin must be defined in first pass NOO TODO TODO TODO TODO
                 errors.push({ errorType : "missingReferencedMixins", mixin : rootMixinName,
                       field : fieldPath, intermediateMixin : mixin, subfield : fieldName,
                       resourceType : subFieldResourceType }); // setting up another loop on fields
                 continue fieldsLoop;
              }
              // following the link in the import plan :
              var subFieldNameTreeCur = fieldNameTreeCur[fieldName];
              if (typeof subFieldNameTreeCur !== 'object' || subFieldNameTreeCur == null) { // null meaning resource field or first pass happened
                 subFieldNameTreeCur = {};
                 fieldNameTreeCur[fieldName] = subFieldNameTreeCur;
              }
              fieldNameTreeCur = subFieldNameTreeCur;
           }
           // last field path element :
           var fieldName = fieldPathElements[fieldPathElements.length - 1];
           if (fieldName.indexOf(":") == -1) {
              var fieldMixinPrefix = mixin["dcmo:name"].replace(":", "_").replace("_" + importState.mixinMajorVersion, ""); // TODO better ; NO "." else KO in query (and tree path parsing)
              fieldName = fieldMixinPrefix + ":" + fieldName; // buildFieldName()
           }
           // will be filled once field is built
           
           var field = importField(fieldRow, fieldName, fieldNameTreeCur, mixin, mixinTypeName, importState);
           
           // TODO also mixins, resource links & sub...
        }
        
      } while (errors.length != 0);

      importState.model["loops"] = loopIndex + 1;
      importState.model["errors"] = errors;
        
        // CUSTOM or import models (and not fields)
        // TODO OR RATHER binding than script ??!
        //mixins["elec:Canton_0"]["dcmo:idGenJs"] = "";
        //mixins["elec:ElectoralList_0"]["dcmo:idGenJs"] = "";
        //mixins["elec:Elector_0"]["dcmo:idGenJs"] = "'fr' + '/' + r['elec:City_0:CityINSEECode'] + '/' + r['elec_Elector:ElectorNumberInTheCity']";
        //mixins["elec:PollingStation_0"]["dcmo:idGenJs"] = "r['elec_PollingStation:PollingStationID']";
        //mixins["elec:Street_0"]["dcmo:idGenJs"] = ""; // NOOO mixin ; TODO id (?)
        if (typeof mixins["elec:City_0"] !== 'undefined') mixins["elec:City_0"]["dcmo:idGenJs"] = "'/' + r['elec_City:CityINSEECode'] + '/' + r['elec_City:CityName']";
        //mixins["elec:Department_0"]["dcmo:idGenJs"] = ""; // link only
        if (typeof mixins["elec:Country_0"] !== 'undefined') mixins["elec:Country_0"]["dcmo:idGenJs"] = "'fr'"; // NOOO foreigners
        
        // making arrays of fields, fieldAndMixins, mixins :
        var modelOrMixinArray = [];
        var mixinArray = [];
        var modelArray = [];
        for (var mInd in mixins) {
            var mixin = mixins[mInd];
            
            // copy mixin to array element :
            var modelOrMixinArrayElt = {};
            for (var key in mixin) {
               if (key.lastIndexOf("importconf:", 0) === 0) { // startsWith
                  continue; // skip import-only fields that are not defined in server
               }
               modelOrMixinArrayElt[key] = mixin[key];
            }
            if (typeof mixin["dcmo:collectionName"] === 'undefined') {
               mixinArray.push(modelOrMixinArrayElt);
            } else {
               modelArray.push(modelOrMixinArrayElt);
            }
            modelOrMixinArray.push(modelOrMixinArrayElt);
            
            var fieldAndMixinNames = []; // fieldAndMixins (orders their overrides)
            for (var mKey in mixin["dcmo:mixins"]) {
               var mixinName = mixin["dcmo:mixins"][mKey];
               fieldAndMixinNames.push(mixinName);
            }
            var fieldArray = [];
            for (var fieldName in mixin["dcmo:fields"]) {
               var field = mixin["dcmo:fields"][fieldName];
               // copy field to clean it up from import conf :
               var cleanField = {};
               for (var key in field) {
                  if (key.lastIndexOf("importconf:", 0) === 0) { // startsWith
                     continue; // skip import-only fields that are not defined in server
                  }
                  cleanField[key] = field[key];
               }
               fieldArray.push(cleanField);
               fieldAndMixinNames.push(fieldName);
            }
            // TOOO LATER also mixins in fieldAndMixinNames
            modelOrMixinArrayElt["dcmo:fields"] = fieldArray;
            modelOrMixinArrayElt["dcmo:fieldAndMixins"] = fieldAndMixinNames;
            mixin["dcmo:fieldAndMixins"] = fieldAndMixinNames; // enriching
        }
        importState.model["modelOrMixinArray"] = modelOrMixinArray;
        importState.model["mixinArray"] = mixinArray;
        importState.model["modelArray"] = modelArray;
        
        ///var results = eval('[' + data.content.data + ']')[0];
        //var prettyJson = toolifyDcResource(results, 0);
        //var mixinsPrettyJson = JSON.stringify(modelOrMixinArray, null, '\t').replace(/\n/g, '<br>');
        var mixinsPrettyJson = toolifyDcResource(modelOrMixinArray, 0);
        $('.importedResourcesFromCsv').html(mixinsPrettyJson);

      callback(importState);
   }
   
   function refreshAndSchedulePost(modelOrMixinArray, relativeTypeUrl, postedCallback) {
      for (var mInd in modelOrMixinArray) {
         var mixin = modelOrMixinArray[mInd];
         // posting one at a time rather than all at once because version has
         // to be refreshed and it is easier to do it in sync this way
         var uri = mixin["@id"];
         var relativeUrl = uri.substring(uri.indexOf("/dc/type/"));
         getData(relativeUrl, function (resource) {
            // updating existing resource : 
            // NB. can't access "mixin" variable because has been changed since call is async
            var upToDateMixin = findMixin(resource["dcmo:name"], modelOrMixinArray);
            upToDateMixin["o:version"] = resource["o:version"];
            postAllDataInType(relativeTypeUrl, JSON.stringify([ upToDateMixin ], null, null),
                  postedCallback, postedCallback);
         }, function (data) {
            // creating new resource :
            var relativeUrl = data.request.path
            var resourceIri = relativeUrl.substring(relativeUrl.indexOf("/dc/type/") + "/dc/type/".length);
            var typeName = decodeURIComponent(resourceIri.substring(resourceIri.indexOf("/") + 1)); // else "elec%3ADepartment_0" ; AND NOT decodeURI
            var upToDateMixin = findMixin(typeName, modelOrMixinArray);
            postAllDataInType(relativeTypeUrl, JSON.stringify([ upToDateMixin ], null, null),
                  postedCallback, postedCallback);
         });
      }
   }
   
   function importModelAndResources() {
      $('.modelRowCounter').html("");
      $('.modelCounter').html("");
      inited = true;
      
      var importState = {
            // CUSTOM
            typeMap : { // TODO more
                  "string" : "string",
                  "integer" : "int",
                  "date" : "date",
                  "list" : "list"
            },
            domainPrefix : 'elec', // first three letters of model import file, changed on file select by UI calling buildModelDomainPrefix()
            mixinMajorVersion : 0, // TODO better
            metamodel : {},
            model : {
               fileName : '', // set below
               modelOrMixins : {}, // NOOOO MUST NOT BE USED outside of csvToModel because have no global fields, rather use .data.involvedMixins
               mixinNameToFieldNameTree : {},
               fieldNamesWithInternalNameMap : {}, // used as set to get all models or mixins that are imported
               modelOrMixinArray : null, // MUST NOT BE USED outside of csvToModel because have no global fields, rather use .data.involvedMixins
               mixinArray : null, // MUST NOT BE USED outside of csvToModel because have no global fields, rather use .data.involvedMixins
               modelArray : null, // MUST NOT BE USED outside of csvToModel because have no global fields, rather use .data.involvedMixins
               loops : 0,
               toBePostedNb : 0,
               postedNb : 0,
               warnings : [],
               errors : [],
               errorHtml : ""
            },
            data : {
               fileName : '', // set below
               dataColumnNames : null,
               involvedMixins : [],
               resources : {},
               toBePostedNb : 0,
               postedNb : 0,
               warnings : [],
               errors : [],
               errorHtml : ""
            }
      }
      
      findDataByType("/dc/type/dcmo:model_0?dcmo:name=$regexdcm.*", function (resources) {
         for (var mmInd in resources) {
            var modelResource = resources[mmInd];
            importState["metamodel"][modelResource["dcmo:name"]] = modelResource;
         }
         findDataByType("/dc/type/dcmi:mixin_0?dcmo:name=$regexdcm.*", function (resources) {
            for (var mmInd in resources) {
               var mixinResource = resources[mmInd];
               importState["metamodel"][mixinResource["dcmo:name"]] = mixinResource;
            }
         
         
      var modelParsingConf = {
         download: true,
         header: true,
         //preview: 3, // TODO !
         complete: function(results) {
            var prettyJson = JSON.stringify(results.data, null, '\t').replace(/\n/g, '<br>');
            $('.importedJsonFromCsv').html(prettyJson);
            
            csvToModel(results.data, importState, function (importState) {
               var mixins = importState.model.modelOrMixins;
               var message = "Handled " + Object.keys(mixins).length + " models or mixins in "
                     + importState.model.loops + " loops";
               if (importState.model.errors.length != 0) {
                  message += ". Aborting, please patch errors:<br/>"
                        + JSON.stringify(importState.model.errors, null, '\t');
               }
               $('.modelRowCounter').html(message);
               if (importState.model.errors.length != 0) {
                  return; // aborting
               }
               
               function fillDataWhenAllModelsUpdated(resourcesOrData) {
                  importedResourcePosted(resourcesOrData, importState.model, importState,
                        "models or mixin", $('.modelCounter'), fillData);
               };

               importState.model["toBePostedNb"] = importState.model.modelOrMixinArray.length;
               refreshAndSchedulePost(importState.model.mixinArray, '/dc/type/dcmi:mixin_0', fillDataWhenAllModelsUpdated);
               refreshAndSchedulePost(importState.model.modelArray, '/dc/type/dcmo:model_0', fillDataWhenAllModelsUpdated);
            });
         }
      };


      importState.domainPrefix = $(".domainPrefix").val(); // changed by UI on model file select to its first three letters, else elec
      importState.model.fileName = $(".modelFile").val();
      if (importState.model.fileName !== "") {
         $(".modelFile").parse({ config : modelParsingConf });
      } else {
         importState.model.fileName = "samples/openelec/oasis-donnees-metiers-openelec.csv";
         Papa.parse(importState.model.fileName + "?reload="
               + new Date().getTime(), modelParsingConf); // to prevent browser caching
      }

         });
      });
      return false;
   }
