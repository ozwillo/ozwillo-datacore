//var containerUrl = "http://data.ozwillo.com/"; // rather in dcConf filled at init by /dc/playground/configuration

   
//////////////////////////////////////////////////:
// DATA MANIPULATION
   
   // generate ids for URIs :
   // * hashing confidential info for uri id, even securely, is bad practice (since the uri is widely exposed).
   // Moreover generating hashes from non-confidential info makes no sense (since not confidential, hashing is not required),
   // save to get a shorter version of it, which is a different use case that is addressed by http://hashids.org ).
   // * id unicity (across time, users, shards...) is actually the first (and only, see above) property to be ensured.
   
   // MOST CASES generate unique (not secure) hash-like id :
   // TODO murmur3 https://github.com/garycourt/murmurhash-js and see discussions
   // at http://hashids.org/ and also http://instagram-engineering.tumblr.com/post/10853187575/sharding-ids-at-instagram
   // and http://stackoverflow.com/questions/7616461/generate-a-hash-from-string-in-javascript-jquery
   
   // IF URI ID IS REALLY TOO LONG generate unique short (hash or two-way) (not secure) id :
   // (unique as long as encoded info is unique. Else make it unique using other info ex. murmur3, see above)
   // TODO two-way : http://hashids.org (2-way, for numbers only)
   // TODO hash : md5, sha1...
   
   // TODO IF NEEDED also base64 encode it

   // IF REALLY IT IS NEEDED generate secure hash id :
   // (make it unique using other info ex. murmur3, see above)
   // TODO SHA256 https://bitwiseshiftleft.github.io/sjcl/ , see also http://stackoverflow.com/questions/18338890/are-there-any-sha-256-javascript-implementations-that-are-generally-considered-t
   // and https://crackstation.net/hashing-security.htm
   // full TLS impl https://github.com/digitalbazaar/forge
   // https://code.google.com/p/crypto-js/
   
   var hashids = new Hashids("Ozwillo Datacore Playground Import Tool"); // https://github.com/ivanakimov/hashids.js 298e9a7f8241f074256f50f0ffa3631f8f4f03e1
   // examples :
   //var id = hashids.encode(1, 2, 3);
   //var numbers = hashids.decode(id);
   
   // Java String.hashCode() see http://stackoverflow.com/questions/7616461/generate-a-hash-from-string-in-javascript-jquery
   function hashCodeId(s) {
      return '' + hashCode(s); // convert to string
   }
   function hashCode(s) {
      var hash = 0, i, chr, len;
      if (s.length == 0) return hash;
      for (i = 0, len = s.length; i < len; i++) {
         chr   = s.charCodeAt(i);
         hash  = ((hash << 5) - hash) + chr;
         hash |= 0; // Convert to 32bit integer
      }
      return hash;
   }
   /*String.prototype.hashCode = function() {
      var hash = 0, i, chr, len;
      if (this.length == 0) return hash;
      for (i = 0, len = this.length; i < len; i++) {
         chr   = this.charCodeAt(i);
         hash  = ((hash << 5) - hash) + chr;
         hash |= 0; // Convert to 32bit integer
      }
      return hash;
   };*/


  // first four letters of model import file
  function buildModelDomainPrefix(modelFileName) {
     var fSepI = modelFileName.lastIndexOf('\\'); // in case of windows
     if (fSepI === -1) { // linux ?!
         fSepI = modelFileName.lastIndexOf('/');
     }
     return modelFileName.substring(fSepI + 1, 3).toLowerCase();
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
      "map" : identity,
      "i18n" : identity, // list of map
      // numbers : see http://stackoverflow.com/questions/5450012/how-to-convert-a-string-to-long-in-javascript
      "boolean" : function(stringValue) {
         return typeof stringValue === 'string' && "true" === stringValue.trim().toLowerCase();
      },
      "int" : function(stringValue) { return parseInt(stringValue, 10); },
      "float" : parseFloat,
      "long" : identity, // javascript has no long, though int ones could be parsed
      "double" : identity, // idem
      "list" : function(stringValue, mixinField, importState) {
         var values = [];
         var stringValues = stringValue.split(",");
         var listElementField = mixinField["dcmf:listElementField"];
         for (var svInd in stringValues) {
            values.push(convertValue(stringValues[svInd], listElementField, importState));
         }
         return values;
      }
      // TODO i18n (list of maps) (not here : map, subresource)
   }
   function convertValue(stringValue, mixinField,
         importState) { // optional, allows to use state when importconf:evalAsJs
      if (typeof stringValue === 'undefined' || stringValue == null) {
         return null; // TODO raise error, shouldn't happen in CSV !(?)
      }
      stringValue += ''; // convert to string if ex. number
      var fieldType = mixinField["dcmf:type"];
      if ("string" === fieldType || "i18n" === fieldType) {
         if (stringValue.length === 0) { // but don't trim
            return null; // empty is none
         }
         if (mixinField["importconf:evalAsJs"]) {
            return eval(stringValue);
         } else if (mixinField["importconf:jsFunctionToEval"]) {
            return eval(mixinField["importconf:jsFunctionToEval"])(stringValue);
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
      return convertFunction(stringValue, mixinField, importState);
   }
   
   function getDefaultValueIfAny(mixinField, importState,
         defaultStringValue) { // optional
      if (typeof defaultStringValue === 'undefined') {
         defaultStringValue = mixinField["dcmf:defaultStringValue"];
      }
      if (typeof defaultStringValue === 'undefined') {
         var genericDefaultValue = convertImportConfValue(importState.model.defaultRow["defaultValue"], 'string');
         if (typeof genericDefaultValue !== 'undefined' && genericDefaultValue !== null) {
            defaultStringValue = genericDefaultValue;
         }
      }
      if (typeof defaultStringValue !== 'undefined') {
         // set default value when not among imported fields (no internalName) :
         if (defaultStringValue.charAt(0) == "\"" && defaultStringValue.charAt(defaultStringValue.length - 1) == "\"") {
            // handling Datacore server-serialized stringValue :
            defaultStringValue = defaultStringValue.substring(1, defaultStringValue.length - 1);
         }
         return convertValue(defaultStringValue, mixinField, importState);
      }
      return null;
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
   
   var globalDefaultLanguage = 'en';
   
   function getDefaultLanguage(i18nField, importState,
         mixin) { // for log purpose
      var defaultLanguage = i18nField['dcmf:defaultLanguage'];
      if (typeof defaultLanguage === 'undefined') {
         var genericDefaultLanguage = convertImportConfValue(importState.model.defaultRow["defaultLanguage"], 'string');
         if (typeof genericDefaultLanguage !== 'undefined' && genericDefaultLanguage !== null) {
            defaultLanguage = genericDefaultLanguage;
         }
      }
      if (typeof defaultLanguage === 'undefined') {
         defaultLanguage = globalDefaultLanguage;
         resourceWarning(importState, 'i18nHasNoDefaultLanguage',
               { fieldName : i18nField['dcmf:name'] });
      }
      return defaultLanguage;
   }
   
   // on primitive values only
   function contains(list, obj) {
      var i = list.length;
      while (i--) {
         var item = list[i];
         if (item === obj) {
            return true;
         }
      }
      return false;
   }
   
   function containsDeepEquals(list, obj) {
      var i = list.length;
      while (i--) {
         var item = list[i];
         if (deepEquals(item, obj)) {
            return true;
         }
      }
      return false;
   }

   // at least a must not be undefined
   function deepEquals(a, b) {
      if (a === b) {
         return true;
      }
      if (a === null || b === null || typeof a !== 'object' || typeof b !== 'object') {
         return false;
      }
      if (Object.keys(a).length !== Object.keys(b).length) {
         return false; // shortcut
      }
      for (var i in a) {
         if (!deepEquals(a[i], b[i])) {
            return false;
         }
      }
      for (var i in b) {
         if (typeof b[i] === undefined) {
            return false;
         }
      }
      return true;
   }
   
   // at least a must not be undefined
   /*function merge(a, b) {
      if (a === b) {
         return a;
      }
      if (a === null || b === null || typeof a !== 'object' || typeof b !== 'object') {
         return null;
      }
      for (var i in a) {
         var aVal = a[i];
         var bVal = b[i];
      }
      for (var i in b) {
         if (!equals(a[i], b[i])) {
            return false;
         }
      }
      return true;
   }*/
   
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
            importState.data.involvedMixins[modelOrMixinResourceOrName] : modelOrMixinResourceOrName;
      return contains(modelOrMixinResource["dcmo:globalMixins"], mixinName);
   }

   // returns first found but adds error if several
   // WARNING may still be wrong if only one, if not yet parsed the right one in the row
   function findBestMatchingRowResourceForType(resourceType, modelTypeToRowResources, importState) {
      if (importState.data.row.loopIndex === 0) {
         resourceError(importState, 'shouldNotCallInFirstLoopFindBestMatchingRowResourceForType');
         return null;
      }
      if (modelTypeToRowResources === null) {
         return null;
      }
      var bestRowResource = modelTypeToRowResources[resourceType];
      if (typeof bestRowResource !== 'undefined') {
         return bestRowResource; // exact match
      }
      var foundRowModelOrMixinName = null;
      for (var rowModelOrMixinName in modelTypeToRowResources) {
         var rowModelOrMixin = importState.model.modelOrMixins[rowModelOrMixinName];
         if (typeof rowModelOrMixin === 'undefined') {
            resourceError(importState, 'rowModelOrMixinNameNotAmongInvolvedMixins',
                  { rowModelOrMixinName : rowModelOrMixinName });
            return null;
         }
         
         // NB. can't be an exact match (has been tested above), so look among mixins :
         var globalMixins = rowModelOrMixin['dcmo:globalMixins'];
         for (var tInd in globalMixins) { // iterating in the order of inheritance
            var mixinName = globalMixins[tInd];
            if (mixinName === resourceType) {
               if (foundRowModelOrMixinName === null) {
                  foundRowModelOrMixinName = rowModelOrMixinName;
               } else if (modelTypeToRowResources[rowModelOrMixinName]['@id']
                     && modelTypeToRowResources[rowModelOrMixinName]['@id'] === modelTypeToRowResources[foundRowModelOrMixinName]['@id']) {
                  resourceWarning(importState, 'sameRowResourceInSeveralModelTypes', {
                     resourceType : resourceType, rowResources :
                     { foundRowModelOrMixinName : modelTypeToRowResources[foundRowModelOrMixinName],
                          rowModelOrMixinName : modelTypeToRowResources[rowModelOrMixinName] },
                     message : "WARNING sameRowResourceInSeveralModelTypes" });
                  // NB. should not happen anymore
               } else {
                  resourceError(importState, 'moreThanOneCompatibleRowResourceForType', {
                     resourceType : resourceType, rowResources :
                     { foundRowModelOrMixinName : modelTypeToRowResources[foundRowModelOrMixinName],
                          rowModelOrMixinName : modelTypeToRowResources[rowModelOrMixinName] },
                     message : "ERROR moreThanOneCompatibleRowResourceForType" });
                  return null; // else autolinking might get wrong !!
               }
            }
         }
      }
      
      if (foundRowModelOrMixinName !== null) {
         return modelTypeToRowResources[foundRowModelOrMixinName];
      } // TODO PERF
      return null;
   }
   
   // TODO LATER handle different id creation policy in subtypes
   function findResourceByUriId(subresource, mixin, importState) {
      var resourceType = mixin['dcmo:name'];
      var subResourceUri = subresource["@id"];
      var subResourceId = parseUri(subResourceUri).id;
      for (var modelOrMixinName in importState.model.modelOrMixins) {
         var modelOrMixin = importState.model.modelOrMixins[modelOrMixinName];
         if (modelOrMixin['dcmo:isInstanciable']) {
            // NB. can't be exact match, has been tested above not to be instanciable
            var globalMixins = modelOrMixin['dcmo:globalMixins'];
            for (var tInd in globalMixins) { // ((iterating in the order of inheritance))
               var mixinName = globalMixins[tInd];
               if (mixinName === resourceType) { // this modelOrMixin is an instance of resourceType
                  var candidateUri = buildUri(modelOrMixinName, subResourceId);
                  var candidateResource = importState.data.resources[candidateUri];
                  if (typeof candidateResource !== 'undefined') {
                     // merge over existing resource (without overriding especially uri) :
                     // NB. works for fields but also native fields ex. modified...
                     mergeValues(candidateResource, subresource, resourceType, importState);
                     return candidateResource;
                  }
               }
            }
         }
      } // TODO PERF
      return null;
   }
   
   // TODO LATER
   function findResourceOrMapByKeyFieldsInList(newResourceOrMap, existingList, fields, importState) {
      resourceError(importState, 'findResourceOrMapByKeyFieldsInListNotYetImplemented');
      return null;
   }
   // at least existingItem must not be undefined
   function isCompatibleWith(newItem, existingItem, allFields, importState) {
      if (newItem === existingItem) {
         return true;
      }
      if (newItem === null) {
         return true;
      }
      var typeOfNewItem = typeof newItem;
      if (typeOfNewItem === 'undefined') {
         return true;
      }
      if (typeOfNewItem !== 'object' || typeof existingItem !== 'object') {
         return false;
      }
      if (newItem instanceof Array) {
         if (!(existingItem instanceof Array)) {
            return false; // TODO handle it elsewhere
         }
         return true; // either there is a compatible value in the existing list, or a new one can merely be added
      }
      for (var i in existingItem) {
         if (typeof allFields[i] === 'undefined') {
            continue; // native field ex. @type
         }
         if (!isCompatibleWith(existingItem[i], newItem[i])) {
            return false;
         }
      }
      // newItem may have additional key values
      return true;
      //return false; // TODO PERF !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! OK !!!
   }
   // returns first found but adds error if several
   function findResourceOrMapByAllFieldsInList(newResourceOrMap, existingList, allFields, importState) {
      var found = null;
      for (var elInd in existingList) {
         var existingResourceOrMap = existingList[elInd];
         if (isCompatibleWith(newResourceOrMap, existingResourceOrMap, allFields, importState)) {
            if (found === null) {
               found = existingResourceOrMap;
            } else {
               resourceError(importState, 'severalResourceOrMapFoundByAllFields', {
                     found : [ found, existingResourceOrMap ], fields : allFields,
                     message : "ERROR severalResourceOrMapFoundByAllFields" });
               break;
            }
         }
      } // TODO PERF
      return found;
   }

   function mergeValues(existingResourceOrMap, newResourceOrMap, allFields, importState, noError) {
      for (var key in newResourceOrMap) {
         // NB. works for fields but also native fields ex. modified...
         mergeValue(existingResourceOrMap, key, newResourceOrMap[key], allFields, importState, noError);
      }
   }
   function mergeListValue(existingList, newValueElt, nvInd, listElementField, importState, noError) {
      //if (true) return; ///////////////////// TODO PERF ! OK !!!
      var fieldType = listElementField["dcmf:type"];
      if (fieldType === 'list' || fieldType === 'i18n') {
         // list of list or of i18n, TODO LATER
         resourceError(importState, 'listWithinListNotYetImplemented', {
               listOfList : existingList, value : newValueElt, listElementField : listElementField,
               message : "ERROR listWithinListNotYetImplemented" });
         return;
         // for each new list value, try to find in ONE of the existing sublists :
         /*if (!(newValueElt instanceof Array)) {
            resourceError(importState, 'listWithinListNotArray', {
                  listOfList : existingList, value : newValueElt, listElementField : listElementField,
                  message : "ERROR listWithinListNotArray" });
         }
         var listWithinListElementField = fieldType['dcmf:listElementField'];
         for (var nlwlvInd in newValueElt) {
            var newListWithinListValue = newValueElt[nlwlvInd];
            var foundExistingListWithinListValue = null;
            for (var elwlInd in existingList) {
               var existingListWithinList = existingList[elwlInd];
               // TODO or key subfield specified on list field ???
               var existingListWithinListValue = findResourceOrMapByKeyFieldsInList(newListWithinListValue,
                     existingListWithinList, listWithinListElementField, importState);
               if (existingListWithinListValue !== null) {
                  if (foundExistingListWithinListValue !== null) {
                     // unable to decide
                     resourceError(importState, 'listWithinListHasTooManyCandidates', {
                           message : "ERROR listWithinListHasTooManyCandidates" });
                  }
                  foundExistingListWithinListValue = existingListWithinListValue;
               }
            }
            if (foundExistingListWithinListValue !== null) {
               mergeValue(foundExistingListWithinListValue, key, newValue, fields, importState);
               mergeValues(foundExistingListWithinListValue, newResourceOrMap, fields, importState);
            } else {
               
            }
         }*/
      
      } else if (fieldType === 'resource' && typeof newValue === 'object') { // embedded resource TODO LATER better test on getStorage() = concrete resource model & storagePath
         var modelOrMixin = importState.model.modelOrMixins[existingValue['@type'][0]];
         // NB. using "most concrete" existingValue fields as guide
         var subresourceFields = modelOrMixin['dcmo:globalFields'];
         // TODO or key subfield specified on list field ???
         /* NOO for now mere merge
         // TODO or key subfield specified on list field ???
         var existingSubresourceFound = findResourceOrMapByKeyFieldsInList(newValueElt, existingList, subresourceFields, importState);
         */
         var existingSubresourceFound = findResourceOrMapByAllFieldsInList(newValueElt, existingList, subresourceFields, importState);
         if (existingMapFound === null) {
            existingList.push(newValueElt);
         } else {
            mergeValue(existingSubresourceFound, null, newValueElt, subresourceFields, importState, noError);
         }
         
      } else if (fieldType === 'map') {
         /* NOO for now mere merge
         // TODO or key subfield specified on list field ???
         var existingMapFound = findResourceOrMapByKeyFieldsInList(newValueElt, existingList, listElementField, importState);
         */
         var listMapFields = listElementField['dcmf:mapFields'];
         // NB. using "most concrete" existingValue fields as guide
         var existingMapFound = findResourceOrMapByAllFieldsInList(newValueElt, existingList, listMapFields, importState);
         if (existingMapFound === null) {
            existingList.push(newValueElt);
         } else {
            mergeValues(existingMapFound, newValueElt, listMapFields, importState, noError);
         }
         
      } else if (!contains(existingList, newValueElt)) { // primitive value not yet in list
         existingList.push(newValueElt);
      } // else primitive value already in list
   }
   // merge new value over existing resource :
   // also list (primitives as sets, LATER beyond id / key subfields should be used), i18n, map & embedded subresource
   // NB. key param is required only if no or default existing value
   function mergeValue(existingResource, key, newValue, allFields, importState, noError) {
      if (typeof newValue === 'undefined' || newValue == null
            || typeof newValue === 'string' && newValue.length == 0) {
         return; // no new value to merge
      }
      var existingValue = existingResource[key];
      if (typeof existingValue === 'undefined' || existingValue == null
            || typeof newValue === 'string' && existingValue.length == 0 // empty string
            || newValue instanceof Array && existingValue.length === 0) { // empty list
         existingResource[key] = newValue; // TODO more empty, conflicts
         return;
      }
      // NB. if existingValue is an Array, don't automatically push newValue in it as if it were
      // a single value list, check field type first (else translation string goes in i18n list)
      var field = findField(key, allFields);
      if (field === null) {
         return; // native fields ex. @id
      }
      var fieldType = field["dcmf:type"];
      if (fieldType === 'i18n') { // TODO LATER wider field.isSet (in mergeList)
         // OR RATHER embedded subresource with (local ?) @id used as key ! 
         // NB. existingValue has at least one value, because tested above
         var newValueArray = newValue instanceof Array ? newValue : [newValue];
         for (var nvInd in newValueArray) {
            if (!newValueArray[nvInd]['v']) {
               newValueArray[nvInd]['v'] = ''; // rather than merely skip, else couldn't remove translations
               // (but not if no existingValue since mergeValue is not called then)
            }
            for (var elInd in existingValue) {
               if (existingValue[elInd]['l'] === newValueArray[nvInd]['l']) {
                  existingValue[elInd] = newValueArray[nvInd];
                  return;
               }
            }
            // not found, add it :
            existingValue.push(newValueArray[nvInd]);
         }
      } else if (fieldType === 'list') {
         // NB. existingValue has at least one value, because tested above
         if (newValue instanceof Array) {
            for (var nvInd in newValue) {
               mergeListValue(existingValue, newValue[nvInd], nvInd, field["dcmf:listElementField"], importState, noError); // TODO or key subfield specified on list field ???
            }
         } else { // TODO better for non primitive values...
            mergeListValue(existingValue, newValue, -1, field["dcmf:listElementField"], importState, noError);
         }
         
      } else if (fieldType === 'map') {
         mergeValues(existingValue, newValue, field['dcmf:mapFields'], importState, noError);
         
      } else if (fieldType === 'resource' && typeof newValue === 'object') { // embedded resource TODO LATER better test on getStorage() = concrete resource model & storagePath
         var modelOrMixin = importState.model.modelOrMixins[existingValue['@type'][0]];
         // NB. iterating over newValue values, but using "most concrete" existingValue fields as guide
         mergeValues(existingValue, newValue, modelOrMixin['dcmo:globalFields'], importState, noError);
         
      } else if (existingValue === newValue) {
         // nothing to do
      } else if (existingValue === convertValue(field['dcmf:defaultStringValue'], field, importState)) {
         // (including external resource case)
         existingResource[key] = newValue; // allow to override default value
      } else if (!noError) {
         resourceError(importState, 'incompatiblePrimitiveValueMerge', {
               resource : existingResource, key : key, newValue : newValue,
               message : "ERROR incompatiblePrimitiveValueMerge" });
      }
   }

   function convertImportConfValue(stringValue, valueType) {
      if (typeof stringValue === 'undefined' || stringValue == null) {
         return null; // TODO raise error, shouldn't happen in CSV !(?)
      }
      if ("string" === valueType || 'i18n' === valueType) {
         if (stringValue.length === 0) { // but don't trim
            return null; // empty is none
         }
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
   
   function mergeStringValueOrDefaultIfAny(existingResource, key, newStringValue, mixin, importState,
         defaultStringValue) { // optional
       if (!mixin) {
           console.log('err', existingResource, key, newStringValue, mixin, importState,
                   defaultStringValue);
       }
      var mergeField = findField(key, mixin["dcmo:globalFields"]);
      var newValue = convertValue(newStringValue, mergeField, importState);
      if (newValue === null) {
         var existingValue = existingResource[key];
         if (typeof existingValue === 'undefined' || existingValue === null) {
            // no value yet
            if (typeof defaultStringValue !== 'undefined') {
               newValue = convertValue(defaultStringValue, mergeField, importState);
            }
            if (newValue === null) {
               var defaultStringValue = mergeField["dcmf:defaultStringValue"];
               if (typeof defaultStringValue !== 'undefined') {
                  newValue = convertValue(defaultStringValue, mergeField,importState);
               }
            }
         } // else no need to compute default value, there is already one
      }
      mergeValue(existingResource, key, newValue, mixin["dcmo:globalFields"], importState);
   }
   
   
   

   function importedResourcePosted(resourcesOrErrorData, importStateRes, importState, kind, counter, origResources,
         success, error) { // optional
      if (resourcesOrErrorData !== null && typeof resourcesOrErrorData === 'object'
            && typeof resourcesOrErrorData["_raw"] === 'object') {
         // error response
         // adding exactly one error item per request in error :
         //var postedError = { data : resourcesOrErrorData._body._body,
         //      request : resourcesOrErrorData.request, resources : origResources };
         var postedError = { data : resourcesOrErrorData, resources : origResources };
         if (origResources) {
            if (!(origResources instanceof Array)) {
               var origResource = origResources;
               var origResourceUri = origResource['@id'];
               importStateRes.sentResourceUriSet[origResourceUri] = null;
               importStateRes.postedErrors[origResourceUri] = postedError;
            } else {
               for (var rInd in origResources) {
                  var origResource = origResources[rInd];
                  var origResourceUri = origResource['@id'];
                  importStateRes.sentResourceUriSet[origResourceUri] = null;
                  importStateRes.postedErrors[origResourceUri] = postedError;
               }
            }
         } else {
            console.log('WARNING no origResources');
         }
         
      } else {
         // no error (null data when already handled ex. skipped resources), merely displaying posted resources :
         for (var rInd in resourcesOrErrorData) {
            var postedResource = resourcesOrErrorData[rInd];
            var postedUri = postedResource['@id'];
            importStateRes.postedResources[postedUri] = postedResource;
            importStateRes.sentResourceUriSet[postedUri] = null;
            delete importStateRes.postedErrors[postedUri]; // in case retried because of conflict
            importStateRes.postedResourceUris.push(postedUri);
         }
         /*if (typeof resourcesOrData['@id'] !== 'undefined') {
            importStateRes.sentResourceUriSet[resourcesOrData['@id']] = null; // set
         } else { // usual case : single value list
            for (var rInd in resourcesOrData) {
               importStateRes.sentResourceUriSet[resourcesOrData[rInd]['@id']] = null; // set
            }
         }*/
      }
      
      importStateRes.sentNb = Object.keys(importStateRes.sentResourceUriSet).length; // updating
      importStateRes.postedNb = Object.keys(importStateRes.postedResources).length; // updating
      importStateRes.postedErrorNb = Object.keys(importStateRes.postedErrors).length; // updating
      var done = importStateRes.toBePostedNb === importStateRes.sentNb + importStateRes.skippedNb; //  + importStateRes.postedErrors.length);
      // TODO BETTER if last one has to be retried because of conflict
      
      if (true/*importStateRes.sentNb % 10 == 0 || importStateRes.sentNb > importStateRes.toBePostedNb - 10*/) {
         var msg = "Posted <a href=\"#importedResourcesFromCsv\"";
         if (done) {
            var resourcesSummary = '';
            for (var rInd in importStateRes.postedResourceUris) {
               if (rInd < 15 || rInd > importStateRes.sentNb - 10) {
                  var parsedUri = parseUri(importStateRes.postedResourceUris[rInd]);
                  resourcesSummary += '../' + parsedUri.modelType + '/' + parsedUri.id + ' \n';
               } else if (rInd === 15) {
                  resourcesSummary += "...\n";
               }
            }
            msg += " title=\"" + resourcesSummary + "\"";
         }
         msg += ">" + importStateRes.postedNb + /*' / ' + importStateRes.toBePostedNb +*/" " + kind + "s</a> (";
         
         // adding skipped msg :
         msg += 'skipped <a href="#"';
         if (done && importStateRes.skippedNb !== 0) {
            var skippedSummary = 'of models : ' + Object.keys(importStateRes.skippedModelTypeSet).join(', ') + '\n';
            skippedSummary += 'in projects : ' + Object.keys(importStateRes.skippedProjectSet).join(', ') + '.\n';
            skippedSummary += 'resources :\n';
            for (var rInd in importStateRes.skippedResourceUris) {
               if (rInd < 15 || rInd > importStateRes.skippedNb - 10) {
                  var parsedUri = parseUri(importStateRes.skippedResourceUris[rInd]);
                  skippedSummary += '../' + parsedUri.modelType + '/' + parsedUri.id + ' \n';
               } else if (rInd === 15) {
                   skippedSummary += "...\n";
               }
            }
            msg += " title=\"" + skippedSummary + "\"";
         }
         msg += '>' + importStateRes.skippedNb;
         if (importStateRes.skippedNb !== 0) {
            msg += ' of '
                  + Object.keys(importStateRes.skippedModelTypeSet).length + ' models in '
                  + Object.keys(importStateRes.skippedProjectSet).length + ' projects';
         }
         msg += '</a>, ';
         
         // adding errors msg :
         if (importStateRes.postedErrorNb === 0) {
            msg += "no error)";
            if (done) {
               importStateRes.endTime = moment();
               msg += " in " + importStateRes.endTime.diff(importStateRes.startTime, 'seconds') + 's';
            }
            msg += ", <a href=\"#datacoreResources\">browse them</a>";
         } else {
            msg += "<a href=\"#datacoreResources\">" + importStateRes.postedErrorNb + " error"
            if (importStateRes.postedErrorNb !== 1) {
               msg += "s";
            }
            msg += "</a>)";
            if (done) {
               importStateRes.endTime = moment();
               msg += " in " + importStateRes.endTime.diff(importStateRes.startTime, 'seconds') + 's';
            }
         }
         counter.html(msg);
         // updating error details : NOO only at the end
         /*if (importStateRes.postedErrorNb !== 0) {
            $('.mydata').html(importStateRes.errorHtml);
         }*/
      }
      
      if (done) {
         displayImportedResourcesPosted(importStateRes);
         
         if (importStateRes.postedErrorNb == 0) {
            console.log("INFO Successfully posted " + importStateRes.postedNb + " " + kind + "s.");
            if (typeof success !== 'undefined') {
               success(importState);
            }
            
         } else {
            console.log("WARNING Posted " + importStateRes.postedNb + " "+ kind
                  + "s with " + importStateRes.postedErrorNb + " errors.");
            if (typeof error !== 'undefined') {
               error(importState);
            } else {
               return concludeImport();
            }
         }
         return true;
      }
      return false;
   }
   function displayImportedResourcesPosted(importStateRes) {
      if (importStateRes.postedErrorNb == 0) {
         if (importStateRes.postedResourceUris.length !== 0) {
            var lastUri = importStateRes.postedResourceUris[importStateRes.postedResourceUris.length - 1];
            setUrl(lastUri); // pointing to all resources in last posted in type
         } else {
            setUrl('');
         }
         $('.mydata').html(toolifyDcResourcePartial(importStateRes.postedResources, 50)); // , null, parseUri(data.request.path).modelType
         
      } else {
         var partialErrors = getPartial(importStateRes.postedErrors, 10);
         for (var eInd in partialErrors.res) {
            var postedError = partialErrors.res[eInd];
            importStateRes.errorHtml += "<p>-&nbsp;";
            if (postedError.data._body._type === 'application/json') {
               var jsonError = eval('[' + postedError.data._body._body + ']')[0];
               // NB. wrapping by [...] and taking [0] because not list

               importStateRes.errorHtml += jsonError.message; // 'code : ' + error.code
               if (jsonError.causes && jsonError.causes.length !== 0) {
                  importStateRes.errorHtml += "<br/>" + jsonError.causes[0].message; // 'code : ' + error.code
               }
               if (jsonError.projectName) { // Resource & Model Exception
                  importStateRes.errorHtml += '<br/>project : ' + jsonError.projectName + ', ';
               }
               importStateRes.errorHtml += 'location : <br/>' + ((jsonError.causes && jsonError.causes.length !== 0) ?
                     jsonError.causes[0].location : jsonError.location);
               if (jsonError.resource) { // Resource exception
                  importStateRes.errorHtml += '<br/>resource : ' + toolifyDcListOrResource(jsonError.resource);
               } else if (postedError.resources) {
                  importStateRes.errorHtml += '<br/>resource : ' + (postedError.resources['@id'] ?
                        toolifyDcListOrResource(postedError.resources) : toolifyDcResourcePartial(postedError.resources));
               }
               /*if (jsonError.resource) { // Resource exception
                  importStateRes.errorHtml += '<br/>resource : ' + toolifyDcListOrResource(jsonError.resource);
                  if (jsonError.causes && jsonError.causes.length !== 0) {
                     importStateRes.errorHtml += jsonError.causes[0].message; // 'code : ' + error.code
                  } else {
                     importStateRes.errorHtml += jsonError.message; // 'code : ' + error.code
                  }
               } else {
                  importStateRes.errorHtml += jsonError.message; // 'code : ' + error.code
                  if (postedError.resources) {
                     importStateRes.errorHtml += '<br/>resource : ' + (postedError.resources['@id'] ?
                           toolifyDcListOrResource(postedError.resources) : toolifyDcResourcePartial(postedError.resources));
                  }
               }*/
               if (jsonError.model) { // ModelException (MIGHT NOT BE REQUIRED IF ALREADY DISPLAYED IN "resource")
                  importStateRes.errorHtml += '<br/>model : ' + JSON.stringify(jsonError.model, null, "\t") + ', ';
               }
            } else {
               importStateRes.errorHtml += postedError.data._body._body;
               if (postedError.resources) {
                  importStateRes.errorHtml += '<br/>resource : ' + (postedError.resources['@id'] ?
                        toolifyDcListOrResource(postedError.resources) : toolifyDcResourcePartial(postedError.resources));
               }
            }
            importStateRes.errorHtml += "</p>";
         }
         if (partialErrors.isPartial) {
            importStateRes.errorHtml += "<br/>...";
         }
         $('.mydata').html(importStateRes.errorHtml);
      }
   }
   

   // severity : error or warning (log list name id deduced by adding ('s')
   function log(importState, severity, code, detailsMap) {
      var logListName = severity + 's';
      var log = { code : code };
      if (importState.data.row) {
         if (importState.data.row.iteration) {
            logs = importState.data.row.iteration[logListName];
            // iteration default info :
         } else {
            logs = importState.data[logListName];
         }
         // row default info :
         log.path = importState.data.row.pathInFieldNameTree.join('.');
         log.line = importState.data.rInd;
      } else {
         logs = importState.model[logListName];
         // NB. happens when mergeValue() used in model field parsing
      }
      logs.push(log);
      if (detailsMap) {
         for (var key in detailsMap) {
            log[key] = detailsMap[key];
         }
      }
      if (importState.data.row) {
         // post details row default info :
         if (importState.data.detailedErrors) {
            log.resources = importState.data.row.modelTypeToRowResources;
         }
      }
      return log;
   }
   function resourceError(importState, code, detailsMap) {
      return log(importState, 'error', code, detailsMap);
   }
   function resourceWarning(importState, code, detailsMap) {
      return log(importState, 'warning', code, detailsMap);
   }
   
   
   function importMapOrResourceValue(fieldOrListField, resourceRow, subFieldNameTree,
         mixin, fieldName, importState) { // log purpose only
      var fieldOrListFieldType = fieldOrListField["dcmf:type"];
      var resourceType = fieldOrListField["dcmf:resourceType"];
      var resourceMixinOrFields;
      if (fieldOrListFieldType === 'resource') {
         // resource tree : looking for value in an (indirectly) referenced resource
         resourceMixinOrFields = importState.data.involvedMixins[resourceType];
         if (typeof resourceMixinOrFields === 'undefined') {
            resourceError(importState, 'unknownReferencedMixin', { referencedMixin : resourceType,
                  message : "ERROR can't find resource field referenced mixin "
                  + resourceType + " among involved mixins" });
            return null;
         }
      } else if (fieldOrListFieldType === 'map') {
         resourceMixinOrFields = fieldOrListField['dcmf:mapFields'];
      } else {
         resourceError(importState, 'badFieldTypeForImportFieldTreeNode', { fieldType : fieldOrListFieldType,
               message : "ERROR badFieldTypeForImportFieldTreeNode" });
         return null;
      }
      
      // (sub)resource field values might be provided, let's look for them
      var subresource;
      try {
         importState.data.row.fieldNameTreeStack.push(subFieldNameTree);
         //importState.data.row.pathInFieldNameTree.push(resourceType); // NO keep it a dotted field path
         subresource = csvRowToDataResource(resourceMixinOrFields, resourceRow,
               subFieldNameTree, null, importState);
      } catch (e) {
         if (importState.aborted) {
            abortImport(); // not only with csvRowToDataResource() else the Abort message would get overwritten
         } else {
            abortImport(e);
         }
         return null;
      } finally {
         importState.data.row.fieldNameTreeStack.pop();
         //importState.data.row.pathInFieldNameTree.pop(); // NO keep it a dotted field path
      }
      
      if (subresource === null) {
         // (may be solved in next iteration)
         resourceWarning(importState, 'subresourceWithoutValue');
         return null;
         
      } else if (fieldOrListFieldType === 'map') {
         return subresource;
            
      } else if (typeof subresource["@id"] === 'undefined') {
         // (may be solved in next iteration)
         resourceError(importState, 'unableToBuildSubresourceId', { subresource : subresource,
               message : "ERROR unableToBuildSubresourceId" });
         return null;
      }
      
      var subresourceModel = importState.data.involvedMixins[subresource['@type'][0]];
      if (subresourceModel/*resourceMixinOrFields*/['dcmo:isInstanciable']) { // resource with (probably) exact instance model type ;
         // TODO TODO better is subresource ex. storage(Path) == modelx/path ; typeof value !== 'undefined' && value != null && value.length != 0
         // adding it :
         addResource(subresource, mixin, importState, false); // DON'T add to modelTypeToRowResources for autolinking because not top level
         return subresource["@id"]; // uri
         
      } else/* if ()*/ { // resource with abstract type : don't import but look for compatible (sub)type and same id
         subresource = findResourceByUriId(subresource, resourceMixinOrFields, importState);
         return subresource !== null ? subresource["@id"] : null; // uri
         
      }/* else { // subresource ; TODO TODO better is subresource ex. storage(Path) == modelx/path ; typeof value !== 'undefined' && value != null && value.length != 0
         return subresource;
      }*/
   }
   
   // mixin must be dcmo:isInstanciable (old "model")
   function csvRowToDataResource(mixin, resourceRow,
         fieldNameTree, modelTypeToRowResources, importState) {
      var typeName = mixin["dcmo:name"];
      var mixinFields, enrichedModelOrMixinFieldMap;
      var topLevelResource = false;
      if (typeof typeName === 'undefined') {
         // assuming map
         typeName = 'map'; // ??
         mixinFields = mixin;
         enrichedModelOrMixinFieldMap = importState.model.modelOrMixins[importState.data.row.pathInFieldNameTree[0]]['dcmo:fields']; // for import-specific data lookup ; NOT globalFields (??)
         for (var pInd = 1; pInd < importState.data.row.pathInFieldNameTree.length; pInd++) {
            var enrichedSubField = enrichedModelOrMixinFieldMap[importState.data.row.pathInFieldNameTree[pInd]];
            var enrichedSubFieldType = enrichedSubField['dcmf:type'];
            while (enrichedSubFieldType === 'list' || enrichedSubFieldType === 'i18n') {
               enrichedSubField = enrichedSubField['dcmf:listElementField'];
               enrichedSubFieldType = enrichedSubField['dcmf:type'];
            }
            enrichedModelOrMixinFieldMap = enrichedSubField['dcmf:mapFields'];
            // TODO TODO or (sub)resource !!
         }
      } else {
         if (fieldNameTree === null) {
            topLevelResource = true;
            fieldNameTree = importState.model.mixinNameToFieldNameTree[typeName];
         }
         mixinFields = mixin["dcmo:globalFields"];
         enrichedModelOrMixinFieldMap = importState.model.modelOrMixins[typeName]["dcmo:globalFields"]; // for import-specific data lookup
         if (!enrichedModelOrMixinFieldMap) {
             throw "Was not able to retrieve model " + typeName + " from server using query on parsed field names";
         }
      }
      var resource = null;
      if (modelTypeToRowResources != null // means top level resource, parsed with actual concrete instance model type
            // else recursive import along fieldNameTree import plan
            && importState.data.row.loopIndex !== 0) { // else not all top level resources have been parsed yet so might be wrong 
         resource = findBestMatchingRowResourceForType(typeName, modelTypeToRowResources, importState);
      }
      if (resource === null) {
         resource = {};
      }
      var mixinHasValue = false;
      var mixinHasAutolinkedValue = false;
      var mixinHasDefaultValue = false;
      
      //for (var fieldName in (Object.keys(fieldNameTree).length !== 0 ?
      //      fieldNameTree : enrichedModelOrMixinFieldMap)) {
      for (var fieldName in enrichedModelOrMixinFieldMap) {
         if (importState.aborted) {
            throw "aborted";
         }
         
         var fieldOrListField = enrichedModelOrMixinFieldMap[fieldName];
         var fieldOrListFieldType = fieldOrListField["dcmf:type"];
         
         var subFieldNameTree = fieldNameTree[fieldName];
         // stack up subPathInFieldNameTree :
         try {
            importState.data.row.pathInFieldNameTree.push(fieldName);
         var enrichedModelOrMixinField = enrichedModelOrMixinFieldMap[fieldName]; // allows lookup of import-only conf
         
         var value = null;
         var field = fieldOrListField;
         var fieldType = fieldOrListFieldType;
         while (fieldOrListFieldType === 'list' || fieldOrListFieldType === 'i18n') {
            fieldOrListField = fieldOrListField['dcmf:listElementField'];
            fieldOrListFieldType = fieldOrListField["dcmf:type"];
         }
         
         if (subFieldNameTree !== null // else primitive (?)
               && typeof subFieldNameTree === 'object' // ex. {} ; typeof fieldOrListField["dcmf:type"] !== 'resource'
                  && subFieldNameTree['type'] === 'node') { // else leaf (primitive)
            
            if (fieldType === 'list' || fieldType === 'i18n') {
               // import 0, 1... nth list item (map or (embedded or not) resource) if defined in fieldNameTree import plan
               var listItemSubFieldNameTree;
               for (var listItemInd = 0; typeof (listItemSubFieldNameTree = subFieldNameTree[listItemInd + '']) !== 'undefined'; listItemInd++) {
                  var listItemValue = importMapOrResourceValue(fieldOrListField, resourceRow, listItemSubFieldNameTree,
                        mixin, fieldName, importState);
                  
                  if (listItemValue !== null) {
                     mixinHasValue = true;
                     var valueList = resource[fieldName];
                     if (typeof valueList === 'undefined') {
                        if (fieldType !== 'i18n' || listItemValue.v) {
                           valueList = [listItemValue];
                           resource[fieldName] = valueList; // must be set now else would be reput in a list
                        } // else skip empty translation
                        // (otherwise when field is used in id, would create an id containing 'undefined')
                     
                     // NB. to import without specifying language, use default language and don't specify listItemIndex
                     } else if (fieldType === 'i18n') { // TODO LATER wider field.isSet (in mergeList)
                        // OR RATHER embedded subresource with (local ?) @id used as key !
                        mergeValue(resource, fieldName, listItemValue, enrichedModelOrMixinFieldMap, importState); // noError
                     } else {
                        valueList.push(listItemValue);
                     }
                  }
               }
            }
            
            // import regular map or (embedded or not) resource field
            value = importMapOrResourceValue(fieldOrListField, resourceRow, subFieldNameTree,
                  mixin, fieldName, importState);
         }

         // optimization, but doesn't skip much (only primitive value & mainly autolinking lookup) !
         if (value === null && typeof resource[fieldName] !== 'undefined'
               && fieldOrListFieldType !== fieldType) { // list or i18n
            mixinHasValue = true; // TODO or default value ?!!
            continue; // skipping, value already found in this row
            // WARNING NOT FOR MULTIPLE LANGUAGES IN A SINGLE LINE OR SAME FOR LIST
            // Can't skip earlier, because dotted pathes-linked (embedded or linked) resources or map
            // may still need some non-id resource fields to be autolinked
         } // else if value != null will skip anyway and go directly to adding it, TODO LATER skip adding it then when kept in state
         
         if (value === null) {
            // looking for local value :
            // (including if (non sub)resource uri)
            // in column fieldOrListField["importconf:internalName"] or else fieldName
            
            var fieldInternalName = (typeof subFieldNameTree === 'undefined') ?
                  null : subFieldNameTree['importconf:internalName'];
            var unknownFieldInternalName = fieldInternalName === null || typeof fieldInternalName === 'undefined';
            
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
                  value = convertValue(value, fieldOrListField, importState); // NB. empty becomes null
                  // NB. if null, means among imported fields but empty (!fieldHasValue), and default value will be set if any
                  if (value !== null && fieldOrListField['dcmf:type'] === 'resource' && value.indexOf('http') !== 0) {
                     resourceError(importState, 'resourceUriReadInCsvNotHttp',
                           { fieldOrListField : fieldOrListField, value : value });
                  }
               } else {
                  value = null;
                  if (!unknownFieldInternalName) {
                     resourceWarning(importState, 'noColumnForFieldInternalName',
                           { fieldInternalName : fieldInternalName,
                           message : "Can't find column '" + fieldInternalName
                           + "' (field internal name) nor '" + fieldName
                           + "' for field " + fieldName + " in mixin " + typeName });
                  }
               }
            } // else not top level, so has to use an internal field name other than fieldName, so skipping import
            mixinHasValue = mixinHasValue || value !== null;

            
            if (value === null) {
               // trying auto linking :
               ////////////////////// NOO (only if no field internal name, else means it's meant to work this way, even when it disables autolinking)
               var resourceType = fieldOrListField["dcmf:resourceType"];
               if (fieldOrListFieldType === 'resource'
                     && importState.data.row.loopIndex !== 0 // else not all top level resources have been parsed yet so might be wrong
                     && modelTypeToRowResources != null // else within subresource call
                     && typeof subFieldNameTree !== 'undefined' // else import not planned, only in inherited field
                     && subFieldNameTree.type !== 'node' // else planned to be imported through dotted pathes
                     && resourceType !== typeName) { // itself (case of ex. node.parentNode)
                  var rowResource = findBestMatchingRowResourceForType(resourceType, modelTypeToRowResources, importState);
                  if (rowResource !== null) {
                     var uri = rowResource["@id"];
                     if (typeof uri !== 'undefined') { // avoiding incomplete rowResource
                        if (mixin['dcmo:isInstanciable']) { // resource ; TODO TODO better is subresource ex. storage(Path) == modelx/path ; typeof value !== 'undefined' && value != null && value.length != 0
                           value = uri;
                        } else { // map, TODO or subresource
                           value = rowResource;
                        }
                        mixinHasAutolinkedValue = mixinHasAutolinkedValue || value !== null;
                     } // else rowResource id may still be incomplete
                  } // else rowResource not yet parsed
               }
            }
            
         }
         
         if (value === null) {
            // set default value when not among imported fields (no internalName) :
            if (typeof subFieldNameTree !== 'undefined') {
               value = getDefaultValueIfAny(fieldOrListField, importState, subFieldNameTree['importconf:defaultStringValue']);
            } else {
               value = getDefaultValueIfAny(fieldOrListField, importState);
            }
            mixinHasDefaultValue = mixinHasDefaultValue || value !== null;
         }
         
         if (value !== null) {
            if (fieldType === 'list' || fieldType === 'i18n') {
               var valueList = resource[fieldName];
               if (typeof valueList === 'undefined') {
                  valueList = [];
                  resource[fieldName] = valueList;
               }
               if (fieldType === 'i18n') {
                  var defaultLanguage = field['dcmf:defaultLanguage']; // TODO also list of i18n !
                  if (typeof defaultLanguage === 'undefined') {
                     resourceError(importState, 'i18nWithoutLanguageNorDefaultOne');
                     continue;
                  }
                  if (typeof value === 'string') { // importing with default language without .v suffix)
                     value = { l : defaultLanguage, v : value };
                  }
                  // TODO LATER wider field.isSet (in mergeList)
                  // OR RATHER embedded subresource with (local ?) @id used as key !
                  mergeValue(resource, fieldName, value, enrichedModelOrMixinFieldMap, importState); // noError
               } else {
                  valueList.push(value);
               }
               value = valueList;
            }
            resource[fieldName] = value;
         }

         } finally {
            importState.data.row.pathInFieldNameTree.pop();
         }
      } // end field loop
      
      
      if (typeof resource['@id'] === 'undefined' && typeName !== 'map') {
         // create or find & update resource :
         
         var mixinMustBeImported = mixinHasValue
               || mixinHasAutolinkedValue && mixin['dcmo:importAutolinkedOnly']
               // whose default is true in 1. free design phase. In 2. unify phase, default is false
               // and it may be true only for singletons ex. CountryFR/FR (otherwise an id field must be provided in data),
               // allowing to skip trying to import mixins whose internal field names are not among data columns titles)
               || mixinHasDefaultValue && mixin['dcmo:importDefaultOnly'];
               // whose default is true in 1. free design phase. In 2. unify phase, default is false,
               // allowing to skip trying to import mixins whose internal field names are not among data columns titles)
         
         // build id :
         var id = null; // NB. id is encoded as URIs should be, BUT must be decoded before used as GET URI
         // because swagger.js re-encodes (per path element because __unencoded__-prefixed per hack)
         // out of fields
         
         var idEncodedValues = [];
         /*for (var idFieldName in enrichedModelOrMixinFieldMap) {
            var idField = enrichedModelOrMixinFieldMap[idFieldName];
            ///fillIndexToEncodedValue(idField, resource, mixin, mixinMustBeImported, pathInFieldNameTree, importState);
            var indexInId = idField["dcmfid:indexInId"]; // NB. this import-specific conf has been enriched in refreshed involvedMixins' global fields
            if (typeof indexInId === 'number') {*/
         if (mixin['dcmoid:idFieldNames']) {
         for (var fInd in mixin['dcmoid:idFieldNames']) {
            var idFieldName = mixin['dcmoid:idFieldNames'][fInd];
            var idField = enrichedModelOrMixinFieldMap[idFieldName];
         
               var idValue = resource[idFieldName];
               var idEncodedValue;
               if (typeof idValue === 'undefined' || idValue === null || idValue === "") {
                  //console.log("Missing value for id field " + idFieldName
                  //      + ", clearing others (" + Object.keys(indexToEncodedValue).length
                  //      + ") ; below " + importState.data.row.pathInFieldNameTree + " in :");
                  //console.log(resource);
                  if (!mixinMustBeImported) {
                     ///return null; // ex. missing a yet unbuilt link ; nothing to import, skip resource
                     // NB. this way only the root cause is shown
                  } else {
                  /*resourceError(importState, 'missingValueForIdField', // TODO or warning since global error anyway ?
                        { fieldName : idFieldName, resource : resource });*/
                  ///return resource; // abort resource creation in this loop (don't add it to uri'd resources yet)
                  }
                  break;
               } else {
               if (idField["dcmf:type"] === 'resource') { // getting ref'd resource id
                  var uri;
                  if (typeof idValue === 'string') { // resource
                     uri = idValue;
                  } else { // subresource
                     uri = idValue["@id"];
                  }
                  if (typeof uri === 'undefined') {
                     //console.log("Missing uri for resource id field " + idFieldName
                     //      + ", clearing others (" + Object.keys(indexToEncodedValue).length
                     //      + ") ; below " + importState.data.row.pathInFieldNameTree + " in :");
                     console.log(resource);
                     resourceError(importState, 'missingUriForResourceIdField', // TODO or warning since global error anyway ?
                           { fieldName : idFieldName, value : idValue, resource : resource });
                     ///return resource; // without adding it to uri'd resources
                     break;
                  } else {
                  var iri = uri.replace(/^.*\/+dc\/+type\/*/, ""); //uri.substring(uri.indexOf("/dc/type/") + 9);
                  idEncodedValue = iri.substring(iri.indexOf("/") + 1);
                  ///indexToEncodedValue[indexInId] = {v : idEncodedValue, uri : uri };
                  }
               } else if (idField["dcmf:type"] === 'i18n') {
                  var defaultLanguage = getDefaultLanguage(idField, importState, mixin);
                  /*if (typeof defaultLanguage === 'undefined') {
                     resourceError(importState, 'i18nIsInIdButHasNoDefaultLanguage',
                           { fieldName : idFieldName });
                     id = null;
                     ///return null;
                     break;
                  }*/
                  var valueInLanguage = null;
                  for (var listInd in idValue) {
                     if (idValue[listInd].l === defaultLanguage) {
                        valueInLanguage = idValue[listInd].v;
                        break;
                     }
                  }
                  if (valueInLanguage === null) {
                     resourceWarning(importState, 'i18nIsInIdButHasNoValueForDefaultLanguage',
                           { fieldName : idFieldName, resource : resource, defaultLanguage : defaultLanguage });
                     ///return null;
                     break;
                  } else {
                  ///indexToEncodedValue[indexInId] = {v : encodeIdSaveIfNot(valueInLanguage + "", // convert to string (might be number)
                  ///      idField) };
                     idEncodedValue = encodeIdSaveIfNot(valueInLanguage + "", // convert to string (might be number)
                           idField);
                  }
               } else {
                  ///indexToEncodedValue[indexInId] = {v : encodeIdSaveIfNot(idValue + "", // convert to string (might be number)
                  ///      idField) };
                  idEncodedValue = encodeIdSaveIfNot(idValue + "", // convert to string (might be number)
                        idField);
               }
               idEncodedValues.push(idEncodedValue);
            }
            /*}*/
         } // end idField loop
         if (idEncodedValues.length === mixin['dcmoid:idFieldNames'].length) {
            id = idEncodedValues.join('/');
         }
         }
         
         if (id !== null) { // scripted id, only if id fields were all there
            var idGenJs = mixin["dcmo:idGenJs"];
            if (typeof idGenJs === 'string' && idGenJs.length != 0) {
               var r = resource;
               id = encodeURI(decodeURI(eval(idGenJs))); // in case idGenJs forgot do encode (per path element)
               // TODO rather 
            }
         }
         
         if (id === null) { // CUSTOM
            // sample :
            /*if (typeName === 'elec:City_0') {
               id = encodeURI(resourceRow["inseeville"]) + '/' + encodeURI(resourceRow["numero_electeur"]); // TODO TODO !!! in second pass ??
            }*/
         }

         var uri = null;
         var lookupQuery = null;
         if (id !== null) {
            uri = buildUri(typeName, id);
            
         } else if (mixin['dcmoid:lookupQueries']) {
            var lookupQueryFields;
            var lookupUriQuery = null;
            var qField;
            for (var qInd in mixin['dcmoid:lookupQueries']) {
               lookupQueryFields = mixin['dcmoid:lookupQueries'][qInd]['dcmoidlq:fieldNames'];
              if (lookupQueryFields.length !== 0) {
               lookupUriQuery = new UriQuery();
               for (var fInd in lookupQueryFields) {
                  var idFieldName = lookupQueryFields[fInd];
               /*var indexInQuery = idField["dcmfid:indexInQuery"]; // NB. this import-specific conf has been enriched in refreshed involvedMixins' global fields
               if (typeof indexInQuery === 'number') {*/
                  var queryValue = resource[idFieldName];
                  if (typeof queryValue === 'undefined' || queryValue === null || queryValue === "") {
                     //console.log("Missing value for query field " + idFieldName
                     //      + ", clearing others (" + lookupUriQuery.params.length
                     //      + ") ; below " + importState.data.row.pathInFieldNameTree + " in :");
                     //console.log(resource);
                     if (!mixinMustBeImported) {
                        ///return null; // ex. missing a yet unbuilt link ; nothing to import, skip resource
                        // NB. this way only the root cause is shown
                     } else {
                        /*resourceError(importState, 'missingValueForQueryField', // TODO or warning since global error anyway ?
                              { fieldName : idFieldName, resource : resource });*/
                        ///return resource; // abort resource creation in this loop (don't add it to uri'd resources yet)
                     }
                     lookupUriQuery = null; // in case no more loops
                     break; // next lookup query (if any)
                     
                  }
                  qField = enrichedModelOrMixinFieldMap[idFieldName];
                  if (qField["dcmf:type"] === 'resource') { // getting ref'd resource id
                     if (typeof queryValue === 'string') { // resource
                        lookupUriQuery.p(idFieldName, queryValue);
                     } else { // subresource TODO also (list of) map & i18n
                        var uriQueryValue = queryValue["@id"];
                        if (typeof uriQueryValue === 'undefined') {
                           //console.log("Missing uri for resource query field " + idFieldName
                           //      + ", clearing others (" + lookupUriQuery.params.length
                           //      + ") ; below " + importState.data.row.pathInFieldNameTree + " in :");
                           ///console.log(resource);
                           /*resourceError(importState, 'missingUriForResourceQueryField', // TODO or warning since global error anyway ?
                                 { fieldName : idFieldName, value : queryValue, resource : resource });*/
                           lookupUriQuery = null;
                           break; // next lookup query/return resource; // without adding it to uri'd resources
                        } else {
                           lookupUriQuery.p(idFieldName + '.@id', uriQueryValue); // TODO test
                        }
                     }
                  } else if (qField["dcmf:type"] === 'i18n') { // NB. beyond default language, is done like list of map
                     var defaultLanguage = getDefaultLanguage(qField, importState, mixin);
                     /*if (typeof defaultLanguage === 'undefined') {
                        resourceError(importState, 'i18nIsInQueryButHasNoDefaultLanguage',
                              { fieldName : idFieldName });
                        return null;
                     }*/
                     var valueInLanguage = null;
                     for (var listInd in queryValue) {
                        if (queryValue[listInd].l === defaultLanguage) {
                           valueInLanguage = queryValue[listInd].v;
                           break;
                        }
                     }
                     if (valueInLanguage === null) {
                        resourceWarning(importState, 'i18nIsInQueryButHasNoValueForDefaultLanguage',
                              { fieldName : idFieldName, resource : resource, defaultLanguage : defaultLanguage });
                        lookupUriQuery = null;
                        break; // next lookup query
                     } else {
                        // NB. could support querying without knowing the language, but must have a
                        // value criteria that is provided in a given language anyway
                        lookupUriQuery.p(idFieldName + '.v', valueInLanguage);
                        lookupUriQuery.p(idFieldName + '.l', defaultLanguage);
                     }
                  } else {
                     lookupUriQuery.p(idFieldName, queryValue);
                  }
               }
              }
               if (lookupUriQuery !== null) { // else one field was missing (or no fields in query)
                  break; // found a complete query,
                  // NB. multi criteria (ex. "between") not supported for lookup queries
               }
            }
            
            if (lookupUriQuery !== null) {
               lookupQuery = "/dc/type/" + encodeUriPathComponent(mixin['dcmo:name']) + '?' + lookupUriQuery.s();
               var alreadyFoundResource = importState.data.lookupQueryToResource[lookupQuery];
               if (typeof alreadyFoundResource === 'undefined') {
                  importState.data.row.lookupQueriesToRun.push(lookupQuery);
                  resourceWarning(importState, 'lookupQueryToRunInNextIteration',
                     { lookupQuery : lookupQuery, resource : resource });
                  return resource;
               } else if (alreadyFoundResource !== null) {
                  uri = alreadyFoundResource['@id'];
                  // NB. not putting alreadyFoundResource in resources, only using it as id,
                  // but will be reused at resource POST time to avoid reGETting it
               } // else remembered lookupQueryHasNoResult error
            }
         }
         
         if (uri === null) {
            if (!mixinMustBeImported) {
               return null; // ex. missing a yet unbuilt link ; nothing to import, skip resource
               // NB. this way only the root cause is shown
            }
            if (mixin['dcmoid:idFieldNames'] && mixin['dcmoid:idFieldNames'].length !== 0) {
               resourceError(importState, 'missingValueForIdField',
                  { idEncodedValues : idEncodedValues, idFieldNames : mixin['dcmoid:idFieldNames'],
                  resource : resource });
            }
            if (mixin['dcmoid:lookupQueries'] && mixin['dcmoid:lookupQueries'].length !== 0) {
               if (lookupUriQuery === null) {
                  resourceError(importState, 'missingValueForQueryField',
                     { lookupQueries : mixin['dcmoid:lookupQueries'],
                     resource : resource,
                     message : "ERROR missingValueForQueryField" });
               } else {
                  resourceError(importState, 'scheduledLookupQueryToRun',
                     { lookupQuery : lookupQuery, resource : resource });
               }
            }
            // TODO better :
            /*resourceWarning(importState, 'noConfForId', { row : resourceRow,
                  message : "No conf for id, using custom gen - in mixin "
                  + typeName + " and resourceRow " + resourceRow });*/
            return resource;
         }
         
         // creating or merging :
         // NB. uri is encoded as URIs should be, BUT must be decoded before used as GET URI
         // because swagger.js re-encodes (per path element because __unencoded__-prefixed per hack)
         var existingResource = importState.data.resources[uri];
         // TODO TODO subresource case
         if (typeof existingResource === 'undefined') {
            resource["@id"] = uri; // WARNING is wrong if !dcmo:isInstanciable, but id may be right (else requires key fields linking)
            //resource["o:version"] = -1;
            resource["@type"] = [ typeName ];
            // NB. added by caller, at top (row) or subresource import level, depending on embedded or not
            ///return resource;
         } else {
            // merge (this mixin's found values) over existing resource :
            // also list (primitives as sets, LATER beyond id / key subfields should be used), i18n, map & embedded subresource
            mergeValues(existingResource, resource, mixin["dcmo:globalFields"], importState); // works for fields but also native fields ex. modified...
            // NB. ancestors, type & uri have already been built and set at creation
            resource = existingResource;
            ///return resource;
         }
      }


      // complete resource once id built :
      if (typeof resource['@id'] !== 'undefined' && typeName !== 'map') {
         
         // (re)computing ancestors :
         // (only now that id is known i.e. we are able to find or create it, even if already exists in case changes)
         // (done not only when id built, but everytime until no unknown resource in line in case of autolinking)
         if (mixin['dcmo:isInstanciable'] && contains(mixin["dcmo:globalMixins"], "oanc:Ancestor_0")) {
            // && mixin["dcmo:isInstanciable"] only if old "model" (already checked)
            ///var ancestorNb = 1 + (mixin['dcmoid:parentFieldNames'] ? mixin['dcmoid:parentFieldNames'].length : 0);
            /*if (resource['oanc:ancestors'] && resource['oanc:ancestors'].length !== 0) { /// === ancestorNb
               // already computed all ancestors (can't know exact number of ancestors, only of parents)
               
            } else {*/
               var ancestors = [];
               if (!mixin['dcmoid:parentFieldNames']) {
                  // no parent (itself is the only ancestor) ex. Country
                  
               } else {
                  var parentUris = [];
                  for (var fInd in mixin['dcmoid:parentFieldNames']) {
                     var idFieldName = mixin['dcmoid:parentFieldNames'][fInd];
                     var idField = enrichedModelOrMixinFieldMap[idFieldName];
                     
                     var parentValue = resource[idFieldName];
                     if (typeof parentValue === 'undefined' || parentValue === null || parentValue === "") {
                        /*resourceError(importState, 'missingValueForParentField',
                              { fieldName : idFieldName, resource : resource });*/
                     } else {
                        
                        // NB. idField is a resource field, because checked at model parsing time
                        var parentUri;
                        if (typeof parentValue === 'string') { // resource
                           parentUri = parentValue;
                        } else { // subresource (refMixin ???)
                           parentUri = parentValue["@id"];
                        }
                        if (typeof parentUri === 'undefined') {
                           /*resourceError(importState, 'missingUriForResourceParentField',
                                 { fieldName : idFieldName, value : parentValue, resource : resource });*/
                           ///return resource; // without adding it to uri'd resources // NOO not required for creation
                        } else {
                           parentUris.push(parentUri);
                        }
                     }
                  }
               
                  if (mixin['dcmoid:parentFieldNames'].length !== parentUris.length) {
                     resourceWarning(importState, 'missingValueForParentField',
                        { parentUris : parentUris, parentFieldNames : mixin['dcmoid:parentFieldNames'],
                        resource : resource });
                     ///return resource; // no need to prevent it from being added to resources to force it to compute parents again because always recomputed
                     
                  }/* else {*/
                  // NB. parents can be optional (ex. some very few CityFR have no CommunauteDeCommune),
                  // so can't explode if error and must recompute everytime until one iteration after no more
                  // missingIdFieldMixinToResources
                     for (var pInd in parentUris) {
                        var parentUri = parentUris[pInd];
                        var ancestor = importState.data.resources[parentUri];
                        // TODO check if ancestor not known (ex. string resource reference...)
                        if (typeof ancestor === 'undefined') {
                           ancestors = null; // reset since incomplete
                           resourceError(importState, 'cantFindAncestorAmongParsedResources', {
                                 resource : resource, ancestorUri : parentUri });
                        } else if (!hasMixin(ancestor["@type"][0], "oanc:Ancestor_0", importState)) {
                           ancestors = null; // reset since incomplete ; TODO or accept as its own single ancestor ?
                           resourceError(importState, 'ancestorHasNotAncestorMixin', {
                              resource : resource, ancestor : ancestor });
                        } else {
                           var ancestorAncestors = ancestor["oanc:ancestors"];
                           if (typeof ancestorAncestors === 'undefined') {
                              ancestors = null; // reset since incomplete
                              resourceError(importState, 'ancestorHasNotDefinedAncestors', {
                                 resource : resource, ancestor : ancestor });
                           } else {
                              for (var aaInd in ancestorAncestors) {
                                 var parentExists = false;
                                 for (var aInd in ancestors) {
                                    if (ancestorAncestors[aaInd] === ancestors[aInd]) {
                                       parentExists = true;
                                       break;
                                    }
                                 }
                                 if (!parentExists) {
                                    ancestors.push(ancestorAncestors[aaInd]);
                                 }
                              }
                           }
                        }
                     }
                  }
               /*}*/
  
               if (ancestors !== null) {
                  ancestors.push(resource['@id']); // itself, but only if dcmo:isInstanciable, else added at instance building elsewhere 
                  mergeValue(resource, 'oanc:ancestors', ancestors, enrichedModelOrMixinFieldMap, importState);
               }
            ///}
         }
      }
      
      return resource;
   }
   
   function addResource(importedResource, mixin, importState, isTopLevel) {
      var irUri = importedResource["@id"];
      if (typeof irUri === 'undefined') {
         //console.log(JSON.stringify(importedResource, null, null));//
         importState.data.row.iteration.missingIdFieldMixinToResources[mixin["dcmo:name"]] = importedResource;
      } else {
         if (isTopLevel && importState.data.row.modelTypeToRowResources != null) { // top level only, for autolinking
            importState.data.row.modelTypeToRowResources[importedResource['@type'][0]] = importedResource; // in case not yet there
            // and NOT mixin["dcmo:name"] because it may have been changed after calling findBestMatchingRowResourceForType()
         }

         if (!importState.data.resources[irUri]) {
            // adding it :
            importState.data.resources[irUri] = importedResource;
            
            // scheduling refresh, in order not to loose fields that are not imported this time ex. oanc:ancestors :
            importState.data.row.lookupQueriesToRun.push(irUri);
            resourceWarning(importState, 'refreshUriToRunInNextIteration',
               { refreshUri : irUri, resource : importedResource });
         }
      }
   }
           
   function csvToData(importState, getResourceRow, nextRow) {
      /*while*/ if (importState.data.rInd < importState.data.rLength) {
        
         if (importState.data.row === null || importState.data.row.done) {
            importState.data.rowNb++; // nb. rInd is string
            console.log("row " + importState.data.rInd);//
        importState.data.row = {
              loopIndex : 0,
              resourceRow : getResourceRow(importState),
              blockedModelTypeSet : {},
              modelTypeToRowResources : {},
              fieldNameTreeStack : [],
              pathInFieldNameTree : [], // for logging only
              previousMissingIdFieldResourceOrMixinNb : -2,
              missingIdFieldResourceOrMixinNb : -1, // different, else stops
              lookupQueriesToRun : [],
              done : false
         };
         } // else next iteration in same row
        
        // mixins loop :
        /*do {*/
           importState.data.row.iteration = {
                 missingIdFieldMixinToResources : {},
                 errors : [], // NB. assigning is the fastest way to empty an array anyway
                 warnings : []
           };
        
        var importedResource;
        for (var mixinName in importState.data.importableMixins) { // rather than whole involvedMixins
             // for each mixin, find its values in the current row :
             var mixin = importState.data.importableMixins[mixinName];
             
             if (!mixin["dcmo:isInstanciable"]) {
                continue; // don't import models that are strictly mixins ex. oanc:Ancestor_0 or geo:Area_0
             }
             if (importState.data.row.blockedModelTypeSet[mixinName]) {
                continue; // don't import models where no value has been found in this row
             }
             
             try {
                importState.data.row.fieldNameTreeStack.push(importState.model.mixinNameToFieldNameTree[mixinName]);
                importState.data.row.pathInFieldNameTree.push(mixinName);
             importedResource = csvRowToDataResource(mixin, importState.data.row.resourceRow, null,
                   importState.data.row.modelTypeToRowResources, importState);
             } catch (e) {
                if (importState.aborted) {
                   abortImport();
                } else {
                   abortImport(e);
                }
                return;
             } finally {
                importState.data.row.fieldNameTreeStack.pop();
                importState.data.row.pathInFieldNameTree.pop();
             }
             
             if (importedResource == null) {
                // nothing to import (or only autolinked values), remember to skip mixin in next iteration and rows
                importState.data.row.blockedModelTypeSet[mixinName] = mixin;
             } else {
                addResource(importedResource, mixin, importState, true); // DO add to modelTypeToRowResources for autolinking because top level
             }
        }

           if (importState.data.row.previousMissingIdFieldResourceOrMixinNb === 0) {
              importState.data.row.done = true; // next row (last row being ONE ITERATION AFTER everything has been found,
              // so that ex. ancestors can be well computed)
              
           } else if (importState.data.row.missingIdFieldResourceOrMixinNb // size equality enough if involvedMixins don't change
                      == importState.data.row.previousMissingIdFieldResourceOrMixinNb || importState.data.row.loopIndex > 20) {
              resourceError(importState, 'missingIdFields', {
                 missingIdFieldMixinToResources : importState.data.row.iteration.missingIdFieldMixinToResources,
                 row : importState.data.row.resourceRow });
              console.log("csvToData loop aborted, still missing id fields in resources :\n"
                    + JSON.stringify(importState.data.row.iteration.missingIdFieldMixinToResources, null, "\t")
                    + "\n   in resourceRow :\n" + JSON.stringify(importState.data.row.resourceRow, null, null));
              //console.log("   with resources " + JSON.stringify(resources, null, null));
              importState.data.row.done = true; // next row
              
           } else {
              importState.data.row.previousMissingIdFieldResourceOrMixinNb = importState.data.row.missingIdFieldResourceOrMixinNb;
              importState.data.row.missingIdFieldResourceOrMixinNb = Object.keys(importState.data.row.iteration.missingIdFieldMixinToResources).length;
              importState.data.row.loopIndex++;
              if (importState.data.row.lookupQueriesToRun.length === 0) {
                 csvToData(importState, getResourceRow, nextRow); // next iteration
              } else {
                 var lookupQuery = importState.data.row.lookupQueriesToRun.pop();
                 findData(lookupQuery, function(resourcesFound, relativeUrl, data, importState) { // NB. works on query AND GET uri
                    if (resourcesFound) {
                       var resourceFound = null;
                       if (resourcesFound instanceof Array) {
                          if (resourcesFound.length === 0) {
                             importState.data.lookupQueryToResource[lookupQuery] = null; // preventing further lookups
                             importState.data.row.iteration.errors.push({ code : "lookupQueryHasNoResult",
                                lookupQuery : lookupQuery,
                                row : importState.data.row.resourceRow }); // NB. in iteration errors, because might improve next iteration
                             // ex. because uri became buildable that previously depended on another missing autolinked Resource 
                             
                          } else if (resourcesFound.length === 1) {
                             resourceFound = resourcesFound[0];
                             
                          } else {
                             importState.data.errors.push({ code : "lookupQueryHasMoreThanOneResult",
                                lookupQuery : lookupQuery, resourcesFound : resourcesFound,
                                row : importState.data.row.resourceRow }); // NB. not in iteration errors, because won't improve
                          }
                       } else if (typeof resourcesFound === 'object' && resourcesFound['@id']) {
                          resourceFound = resourcesFound;
                       }
                       if (resourceFound !== null) {
                          importState.data.lookupQueryToResource[lookupQuery] = resourceFound;
                          var rfId = resourceFound['@id'];
                          if (importState.data.resources[rfId]) {
                             // already there : merging, in order not to loose fields that are not imported this time ex. oanc:ancestors
                             var modelOrMixin = importState.model.modelOrMixins[resourceFound['@type'][0]];
                             // merge : (gets o:version and missing existing fields, not all @type but recomputed on server anyway)
                             mergeValues(importState.data.resources[rfId], resourceFound, modelOrMixin['dcmo:globalFields'], importState, true);
                          }
                          importState.data.cachedResources[rfId] = resourceFound; // caching for LATER refresh before POSTing
                       }
                    }
                    csvToData(importState, getResourceRow, nextRow); // next iteration
                 }, function(data) {
                    if (lookupQuery.indexOf('?') !== -1) {
                    var error = (data._body && data._body._body) ? data._body._body : data;
                    importState.data.errors.push({ code : "lookupQueryError",
                       lookupQuery : lookupQuery, error : error,
                       row : importState.data.row.resourceRow }); // NB. not in iteration errors
                    } // else mere GET refresh query
                    csvToData(importState, getResourceRow, nextRow); // next iteration
                 }, 0, 2, null, importState); // 2 results are enough to know whether unique
              }
              return;
           }
        
        /*} while (!importState.data.row.done);*/

        for (var i in importState.data.row.iteration.errors) {
           importState.data.errors.push(importState.data.row.iteration.errors[i]);
        }
        for (var i in importState.data.row.iteration.warnings) {
           importState.data.warnings.push(importState.data.row.iteration.warnings[i]);
        }

        if (importState.data.rowNb % 5 == 1) { // or 100 ?
           $('.resourceRowCounter').html("Handled <a href=\"#importedJsonFromCsv\">" + importState.data.rowNb + " rows</a>");
        }
        
        importState.data.rInd++;
        if (nextRow) {
           nextRow(importState, getResourceRow, nextRow); // next row
        }
        
     } else {/**/
        csvToDataCompleted(importState);
     }
  }
  function csvToDataCompleted(importState) {
     importState.data.endTime = moment();
     
      displayParsedResource(importState);

      importState.data.posted.startTime = moment();
      importState.data.posted.toBePostedNb = Object.keys(importState.data.resources).length;
      if (importState.data.posted.toBePostedNb === 0) {
         importedResourcePosted([], importState.data.posted, importState,
               "resource", $('.resourceCounter'), null);
         concludeImport();
      }
      
      function importedDataPosted(resourceOrData, origResources) {
         importedResourcePosted(resourceOrData, importState.data.posted, importState,
               "resource", $('.resourceCounter'), origResources, concludeImport, concludeImport);
      }
      for (var uri in importState.data.resources) {
         if (importState.aborted) {
            return abortImport();
         }
         
         var resource = importState.data.resources[uri];
         if (skipModel(resource, importState.data.posted, importState)
               || skipProject(resource, importState.data.posted, importState)) { // NB. resource has already been refreshed
            continue;
         }
         
         // TODO mass version update !
         getData(uri, function (returnedResource, relativeUrl, data, importState) {
            // existing resource... 
            // NB. can't access original "resource" variable because has been changed since call is async
            var upToDateResourceUri = parseUri(returnedResource["@id"]); // ex. "http://data.ozwillo.com/dc/type/geo%3ACityGroup_0/FR/CC%20les%20Ch%C3%A2teaux"
            // and .modelType ex. "geo:CityGroup_0", .id ex. "FR/CC les Châteaux"
            var upToDateResource = importState.data.resources[upToDateResourceUri.uri];
            if (typeof upToDateResource === 'undefined') {
               console.log("upToDateResource uri", upToDateResourceUri); // should not happen
               importedDataPosted({ _raw : { statusCode : 400 }, _headers:{}, _body:{ _body:
                   'Can\'t find locally resource for posted uri ' + upToDateResourceUri.uri } }, returnedResource); // go on
               return;
            }
            // should we skip posting ?
            ///if (skipProject(returnedResource, importState.data.posted, importState)) {
               // not same project : skip TODO better log & only if security constraint
               /*importedDataPosted({ _raw : { statusCode : 403 }, _headers:{}, _body:{ _body:
                   'Forbidden to post resource ' + returnedResource["@id"] + ' whose model is in project '
                   + resourceModel['dcmo:pointOfViewAbsoluteName'] + ' that is not current one' } }, upToDateResource);*/
               /*importedDataPosted(null, returnedResource); // go on (null data because error already handled)
               return;
            }*/
            // TODO LATER OPT check project again, in case returnedResource has a different modelType ???
            // updating version :
            upToDateResource["o:version"] = returnedResource["o:version"];
            postAllDataInType({ modelType: upToDateResourceUri.modelType }, upToDateResource,
                  importedDataPosted, importedDataPosted);
         }, function (data, relativeUrl, importState) {
            var resourceUri = parseUri(data.request.path); // ex. "/dc/type/geo%3ACityGroup_0/FR/CC%20les%20Ch%C3%A2teaux"
            // and .modelType ex. "geo:CityGroup_0", .id ex. "FR/CC les Châteaux"
            var upToDateResource = importState.data.resources[resourceUri.uri];
            // checking error first (rights...) :
            if (data._raw.statusCode !== 404) { // TODO more specific 403 Forbidden ?
               importedDataPosted(data, upToDateResource);
               return;
            } // else does not exist
            // TODO better skipping error
            // so creating new resource :
            postAllDataInType({ modelType: resourceUri.modelType }, upToDateResource,
                  importedDataPosted, importedDataPosted);
         }, null, importState);
      }
   }
  
   // must be applied on an already retrieved resource
   function getProjectOfResource(resource, importState) {
      return resource['dcmo:pointOfViewAbsoluteName'] // case of model resource
            || importState.model.modelOrMixins[resource['@type'][0]]['dcmo:pointOfViewAbsoluteName'];
      // (wouldn't be found if dcmo:model_0, rather in importState.metamodel)
   }
  
   // must be applied on an already retrieved resource
   function skipProject(resource, importStateRes, importState) {
      var project = getProjectOfResource(resource, importState);
      if (project !== importState.project) { // skip project
         var modelType = resource['dcmo:name'] // case of model resource
               || resource['@type'][0];
         importStateRes.skippedProjectSet[project] = true;

         importStateRes.skippedResourceUris.push(resource["@id"]);
         importStateRes.skippedNb++; // to fullfill "done" condition
         importStateRes.skippedModelTypeSet[modelType] = true;
         return true;
      }
      return false;
   }
  
   function skipModel(resource, importStateRes, importState) {
      var modelType = resource['dcmo:name'] // case of model resource
            || resource['@type'][0];
      if (!importState.model.importedMixinNameSet[modelType]) { // skip model
         importStateRes.skippedResourceUris.push(resource["@id"]);
         importStateRes.skippedNb++; // to fullfill "done" condition
         importStateRes.skippedModelTypeSet[modelType] = true;
         return true;
      }
      return false;
   }
  
   function skipForbidden(resource, importStateRes, importState) {
      if (resource instanceof Array) { // because arg of postAllDataInType()
          resource = resource[0];
      }
      var modelType = resource['dcmo:name'] // case of model resource
            || resource['@type'][0];
      if (importStateRes.skipForbidden) { // skip
         importStateRes.skippedResourceUris.push(resource["@id"]);
         importStateRes.skippedNb++; // to fullfill "done" condition
         importStateRes.skippedModelTypeSet[modelType] = true;
         return true;
      }
      return false;
   }
      
   function displayParsedResource(importState) {
      $('.resourceRowCounter').html("Handled <a href=\"#importedJsonFromCsv\">" + importState.data.rowNb
            + " rows</a> (<a href=\"#datacoreResources\">" + importState.data.errors.length + " errors</a>) in "
            + ((importState.data.endTime === null) ? "?" : importState.data.endTime.diff(importState.data.startTime, 'seconds')) + "s");
      if (importState.data.errors.length != 0) {
         //$('.mydata').html(
         var errorsString = stringifyPartial(importState.data.errors, 50);
         var warningsString = stringifyPartial(importState.data.warnings, 25);
         $('.importedResourcesFromCsv').html("<b>Errors :</b><br/>" + errorsString
               + "<br/><br/><b>Warnings :</b><br/>" + warningsString);
         
      } else {
         // display full resources only if no errors to display :
         //var resourcesPrettyJson = JSON.stringify(resources, null, '\t').replace(/\n/g, '<br>');
         var resourcesPrettyJson = toolifyDcResourcePartial(importState.data.resources, 50);
         $('.importedResourcesFromCsv').html(resourcesPrettyJson + "<br/>...");
      }
   }
   
   function fillData(importState) {
      if (importState.data.rowLimit == 0) {
         return;
      } // else > 0, or -1 means all ; or 
      
      resetResourceCounters();
      //console.log("fillData");//
      var resourceParsingConf = {
            download: true,
            header: true,
            preview: importState.data.rowStart + importState.data.rowLimit + 1, // ex. '1' user input
            // means importing title line + 1 more line (first data line)
            /*step: function(row, handle) {
               console.log("Row:", row.data);
               handle.pause();
               importState.data.rows = row.data;
               if (!importState.data.columnNames) {
                  importState.data.columnNames = row.meta.fields;
                  getImportedFieldsModels(importState, function() {
                     csvToData(importState, asyncGetResourceRow, function() {
                        handle.resume();
                     });
                  });
               } else {
                  csvToData(importState, asyncGetResourceRow, function() {
                     handle.resume();
                  });
               }
            },*/
            complete: function(results) {
               console.log("Remote file parsed!", results);
               ///var results = eval('[' + data.content.data + ']')[0];
               var prettyJson = stringifyPartial(results.data, 10);
               $('.importedJsonFromCsv').html(prettyJson);
               // TODO handle errors...
               
               var nextRowFunction = function () {
                  setTimeout(function () {
                     csvToData(importState, syncGetResourceRow, nextRowFunction);
                  }, 1);
               };
               
               importState.data.rows = results.data;
               importState.data.rLength = Math.min(importState.data.rows.length, importState.data.rowStart + importState.data.rowLimit);
               importState.data.rInd = importState.data.rowStart;
               
               importState.data.columnNames = results.meta.fields;
               importState.data.startTime = moment();
               getImportedFieldsModels(importState, nextRowFunction); // NB. nextRowFunction starts resource building
            }
      }

      // starting parsing :
      if (importState.data.file) {
         importState.data.file.parse({ config : resourceParsingConf });
      } else {
         // file must be online at this relative location (case of default file)
         Papa.parse(importState.data.fileName + "?reload="
               + new Date().getTime(), resourceParsingConf); // to prevent browser caching
      }
   }

   function syncGetResourceRow(importState) {
      return importState.data.rows[importState.data.rInd + '']; // convert to string !!
   }
   function asyncGetResourceRow(importState) {
      return importState.data.rows['0']; // string !!
   }
   
   function getImportedFieldsModels(importState, success) {// imported field names
      // adding field names with internal field name :
      importState.data.importedFieldNames = [];
      var fieldNamesWithInternalNameMap = importState.model.fieldNamesWithInternalNameMap;
      for (var fnInd in fieldNamesWithInternalNameMap) {
         importState.data.importedFieldNames.push(fnInd);
      }
      // adding prefixed data column names :
      for (var dcnInd in importState.data.columnNames) {
         var dataColumnName = importState.data.columnNames[dcnInd];
         if (dataColumnName.indexOf(':') !== -1 && fieldNamesWithInternalNameMap[dataColumnName] === null) {
            importState.data.importedFieldNames.push(dataColumnName);
         }

         // add its mixin to importableMixins :
         var mixinNameSet = importState.model.fieldInternalNameToMixinNames[dataColumnName];
         if (mixinNameSet) {
            for (var mixinName in mixinNameSet) {
            // importable only if has an internalFieldName among data column names
            var importableMixin = importState.model.modelOrMixins[mixinName];
            if (!importableMixin['dcmo:isInstanciable']) { // TODO error
               resourceError(importState, 'dataColumnNameIsFieldInternalNameOfNonInstanciableMixin',
                     { mixin : importableMixin, dataColumnName : dataColumnName });
            } else {
               importState.data.importableMixins[mixinName] = importableMixin;  
            }
            }
         }
      }
      enrichImportedModelOrMixins(importState, success);
   }
   var dcMaxLimit = 100; // BEWARE limited to 10 by default !!!!!! and 100 max !!
   function enrichImportedModelOrMixins (importState, success, lastEnrichedModelOrMixinName) {
      findDataByType({ modelType : 'dcmo:model_0', query : new UriQuery(
         'dcmo:globalFields.dcmf:name', '$in'
         // globalFields else won't get ex. CountryFR inheriting from Country but with no additional field (??)
         + JSON.stringify(importState.data.importedFieldNames, null, null)
      ).p('dcmo:name', (lastEnrichedModelOrMixinName ? '>' + lastEnrichedModelOrMixinName : '') +  '+').s() },
         function(fieldNameMixinsFound, relativeUrl, data, importState) {
            for (var fnmInd in fieldNameMixinsFound) {
               var involvedMixin = fieldNameMixinsFound[fnmInd];
               var modelOrMixin = importState.model.modelOrMixins[involvedMixin["dcmo:name"]];
               if (typeof modelOrMixin === 'undefined') {
                  continue; // not used, ex. unused mixin that inherits a used field
               }
               importState.data.involvedMixins[involvedMixin['dcmo:name']] = involvedMixin;
               
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

               // add to importableMixins if default importable (even without data) : 
               if (involvedMixin['dcmo:isInstanciable']
                     && (involvedMixin['dcmo:importDefaultOnly'] // && has default value (logically)
                     || involvedMixin['dcmo:importAutolinkedOnly'])) {
                  importState.data.importableMixins[involvedMixin["dcmo:name"]] = involvedMixin;
               }
            }
            
            if (fieldNameMixinsFound.length === dcMaxLimit) { // max limit
               //return abortImport("Too many mixins (>= 100) found for field names to import");
               lastEnrichedModelOrMixinName = fieldNameMixinsFound[fieldNameMixinsFound.length - 1]['dcmo:name'];
               enrichImportedModelOrMixins(importState, success, lastEnrichedModelOrMixinName);
            } else if (success) {
               success(importState);
            }
      }, null, 0, dcMaxLimit, null, importState);
   }
   
  
   function importField(fieldRow, fieldName, mixinFields, mixinTypeName, importState) {
      var field = mixinFields[fieldName];
      if (typeof field === 'undefined') {
         var fieldUri = buildUri("dcmf:field_0", mixinTypeName + "/" + fieldName); // TODO map & sub sub resource
         var fieldDataType = fieldRow["Data type"];
         if (typeof fieldDataType === 'undefined' || fieldDataType.trim().length === 0) {
            fieldDataType = 'string'; // if none, defaults to string
         }
         var fieldType = importState.typeMap[fieldDataType.toLowerCase()];
         if (typeof fieldType === 'undefined') {
            fieldType = "resource";
         }
         var fieldOrListField = {
               "dcmf:name" : fieldName, // TODO prefix, case
               "@id" : fieldUri,
               "o:version" : 0, // TODO top level's ?
               "@type" : [ "dcmf:field_0" ], // TODO version, default one ?
               "dcmf:type" : (fieldType === 'i18n') ? 'map' : fieldType
         }; // TODO prefixes, case, version
         
         // TODO case
         if (fieldType === "map") {
            fieldOrListField["dcmf:mapFields"] = {};
         } else if (fieldType === "resource") {
            fieldOrListField["dcmf:resourceType"] = buildMixinName(fieldDataType, importState);
         }
         if (convertMap["boolean"](fieldRow["Multiple"]) || fieldType === 'i18n') {
            field = {
                  "dcmf:name" : fieldName, // TODO prefix, case
                  "@id" : fieldUri,
                  "o:version" : 0, // TODO top level's ?
                  "@type" : [ "dcmf:field_0" ], // TODO version, default one ?
                  "dcmf:type" : (fieldType === 'i18n') ? fieldType : "list",
                  "dcmf:listElementField" : fieldOrListField
            }; // TODO prefixes, case, version
            if (fieldType === 'i18n') {
               fieldOrListField['dcmf:mapFields'] = {
                     'l' : {
                        "dcmf:name" : 'l', // TODO prefix, case
                        "@id" : fieldUri + '/l', // ??
                        "o:version" : 0, // TODO top level's ?
                        "@type" : [ "dcmf:field_0" ], // TODO version, default one ?
                        "dcmf:type" : 'string',
                     },
                     'v' : {
                        "dcmf:name" : 'v', // TODO prefix, case
                        "@id" : fieldUri + '/l', // ??
                        "o:version" : 0, // TODO top level's ?
                        "@type" : [ "dcmf:field_0" ], // TODO version, default one ?
                        "dcmf:type" : 'string',
                     }
               };
            }
         } else {
            field = fieldOrListField;
         }
         // adding it :
         mixinFields[fieldName] = field;
         // TODO trim...
         // TODO checks...
         // TODO also format ex. date
      } // else TODO (LATER) conflicts & merge...

      // more init :
      if (field["dcmf:type"] === "list") {
         var listElementField = field["dcmf:listElementField"];
         mergeStringValueOrDefaultIfAny(listElementField, "dcmf:required", fieldRow["required"],
               importState.metamodel["dcmf:field_0"], importState); // meaningless for list but required by ModelResourceMappingService for now
         mergeStringValueOrDefaultIfAny(listElementField, "dcmf:queryLimit", fieldRow["queryLimit"], importState.metamodel["dcmf:field_0"], importState);
      }
      
      mergeStringValueOrDefaultIfAny(field, "dcmf:required", fieldRow["required"], importState.metamodel["dcmf:field_0"], importState);
      mergeStringValueOrDefaultIfAny(field, "dcmf:aliasedStorageNames", fieldRow["aliasedStorageNames"], importState.metamodel["dcmf:field_0"], importState);
      mergeStringValueOrDefaultIfAny(field, "dcmf:queryLimit", fieldRow["queryLimit"],
            importState.metamodel["dcmf:field_0"], importState); // meaningless for list but required by ModelResourceMappingService for now
      mergeStringValueOrDefaultIfAny(field, "dcmf:isInMixinRef", fieldRow["isInMixinRef"],
            importState.metamodel["dcmf:field_0"], importState); // meaningless for list but required by ModelResourceMappingService for now
      
      mergeStringValueOrDefaultIfAny(field, "dcmf:defaultStringValue", fieldRow["defaultValue"], importState.metamodel["dcmf:field_0"], importState);
      mergeStringValueOrDefaultIfAny(field, "dcmf:defaultLanguage", fieldRow["defaultLanguage"], importState.metamodel["dcmf:field_0"], importState);

      mergeStringValueOrDefaultIfAny(field, "dcmfid:indexInId", fieldRow["indexInId"], importState.metamodel["dcmf:field_0"], importState);      
      mergeStringValueOrDefaultIfAny(field, "dcmfid:indexInParents", fieldRow["indexInParents"], importState.metamodel["dcmf:field_0"], importState);
      if (field['dcmfid:indexInParents'] && field['dcmf:type'] !== 'resource') {
         importState.model.errors.push({ code : "indexInParentsFieldIsNotResource",
            mixin : mixinTypeName, field : field });
      }
      mergeStringValueOrDefaultIfAny(field, "dcmfid:queryNames", fieldRow["queryNames"], importState.metamodel["dcmf:field_0"], importState);
      
      // import conf-specific (not in server-side model) :
      // NB. importconf:internalName & defaultValue are in fieldNameTree import plan
      mergeImportConfStringValue(field, "importconf:dontEncodeIdInUri", fieldRow["dontEncodeIdInUri"], "boolean");
      mergeImportConfStringValue(field, "importconf:evalAsJs", fieldRow["evalAsJs"], "boolean");
      mergeImportConfStringValue(field, "importconf:jsFunctionToEval", fieldRow["jsFunctionToEval"], "string");
      
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
         if (typeof field["dcmfid:indexInId"] === 'undefined' || field["dcmfid:indexInId"] == null) {
            field["dcmfid:indexInId"] = new Number(indexInIdString); // TODO : compute
         }
      }
      setTrimIfAny(field, "dcmf:defaultStringValue", fieldRow["defaultValue"]); // TODO for other apps...
      setTrimIfAny(field, "importconf:internalName", fieldRow["Internal field name"]); // TODO for other apps...
      // TODO if 'undefined' throw bad import conf
      */
      return field;
   }
   
   function buildMixinName(mixinName, importState) {
      // TODO case
      if (mixinName.indexOf(":") == -1) {
         mixinName = importState.domainPrefix + ":" + mixinName; // NO . else KO in query (and tree path parsing)
      }
      if (mixinName.indexOf("_") == -1) {
          mixinName = mixinName + "_" + importState.mixinMajorVersion; // NO . else KO in query (and tree path parsing)
      }
      return mixinName;
   }
   
   function isInteger(n) {
      return !isNaN(parseInt(n)) && isFinite(n);
   }
   
   var mixinTypeName = "dcmo:model_0";
   // callback(mixins, mixinNameToFieldNameTree)
   function csvToModel(resultsData, importState, callback) {
      var mixinNameToFieldNameTree = importState.model.mixinNameToFieldNameTree;
      var mixins = importState.model.modelOrMixins;
        
      var loopMaxIndex = 8; // 3 // 20
      var loopIndex = -1;
      do {
         importState.model.errors = [];
         if (loopIndex > loopMaxIndex) {
            break;
         }
         loopIndex++;
        
        
        // import plan (& internalName) :
        fieldsLoop : for (var fInd in resultsData) {
           if (importState.aborted) {
              return abortImport();
           }
           
           var fieldRow = resultsData[fInd];
           var mixinName = fieldRow["Mixin"];
           if (typeof mixinName === 'undefined' || mixinName === null || mixinName.trim().length === 0) {
              if (Object.keys(importState.model.defaultRow).length === 0) { // no defaultRow yet
                 importState.model.defaultRow = fieldRow; // use empty mixin & field for generic default values
              }
              continue; // skipping import
              // TODO LATER use empty field row for mixin values
           }
           if (mixinName.indexOf('//') === 0 || mixinName.indexOf('#') === 0) {
              continue; // skipping comment line, TODO rm should be done auto by papaparse comments = true
           }
           mixinName = buildMixinName(fieldRow["Mixin"], importState); // TODO case
           
           var mixin = mixins[mixinName];
           ///var isModel = !convertMap["boolean"](fieldRow["Is Mixin"]);
           if (typeof mixin == 'undefined') {
              mixin = {
                 "dcmo:name" : mixinName,
                 "dcmo:pointOfViewAbsoluteName" : importState.project, // TODO LATER + pointOfViewName + '.' + pointOfViewEltName 
                 "@id" : buildUri(mixinTypeName, mixinName),
                 "@type" : [ mixinTypeName ], // NB. there are others but they will be recomputed on server side anyway
                 // more init :
                 "dcmo:majorVersion" : importState.mixinMajorVersion,
                 "dcmo:fields" : {},
                 "dcmo:mixins" : [],
                 "dcmo:fieldAndMixins" : []
              };
              mixins[mixinName] = mixin;
           }
           var mixinFields = mixin["dcmo:fields"];
           
           // POLY
           mergeStringValueOrDefaultIfAny(mixin, "dcmo:isDefinition", fieldRow["isDefinition"],
                 importState.metamodel["dcmo:model_0"], importState, importState.model.defaultRow['isDefinition']); // TODO required
           mergeStringValueOrDefaultIfAny(mixin, "dcmo:isStorage", fieldRow["isStorage"],
                 importState.metamodel["dcmo:model_0"], importState, importState.model.defaultRow['isStorage']); // TODO required
           mergeStringValueOrDefaultIfAny(mixin, "dcmo:isInstanciable", fieldRow["isInstanciable"],
                 importState.metamodel["dcmo:model_0"], importState, importState.model.defaultRow['isInstanciable']); // TODO required
           
           mergeStringValueOrDefaultIfAny(mixin, "dcmo:documentation", fieldRow["documentation"], importState.metamodel["dcmo:model_0"], importState); // TODO required
           mergeStringValueOrDefaultIfAny(mixin, "dcmo:mixins", fieldRow["Has Mixins"], importState.metamodel["dcmo:model_0"], importState);
           mergeStringValueOrDefaultIfAny(mixin, "dcmo:fieldAndMixins", fieldRow["fieldAndMixins"], importState.metamodel["dcmo:model_0"], importState);

           mergeStringValueOrDefaultIfAny(mixin, "dcmo:importDefaultOnly", fieldRow["importDefaultOnly"],
                 importState.metamodel["dcmo:model_0"], importState, importState.model.defaultRow['importDefaultOnly']);
           mergeStringValueOrDefaultIfAny(mixin, "dcmo:importAutolinkedOnly", fieldRow["importAutolinkedOnly"],
                 importState.metamodel["dcmo:model_0"], importState, importState.model.defaultRow['importAutolinkedOnly']);
           
           mergeStringValueOrDefaultIfAny(mixin, "dcmoid:idGenJs", fieldRow["idGenJs"], importState.metamodel["dcmo:model_0"], importState);
           mergeStringValueOrDefaultIfAny(mixin, "dcmoid:useIdForParent", fieldRow["useIdForParent"],
                 importState.metamodel["dcmo:model_0"], importState, importState.model.defaultRow['useIdForParent']);
           
              // storage-level fields : (if not storage nor instanciable, might be used by inheriting models)
              mergeStringValueOrDefaultIfAny(mixin, "dcmo:maxScan", fieldRow["maxScan"], importState.metamodel["dcmo:model_0"], importState);
              mergeStringValueOrDefaultIfAny(mixin, "dcmo:isHistorizable", fieldRow["isHistorizable"], importState.metamodel["dcmo:model_0"], importState);
              mergeStringValueOrDefaultIfAny(mixin, "dcmo:isContributable", fieldRow["isContributable"], importState.metamodel["dcmo:model_0"], importState);
              
           // country / language specific : (requires inheriting a generic mixin)
           var specificToCountryLanguage = fieldRow["specificToCountryLanguage"];
           if (specificToCountryLanguage && specificToCountryLanguage.trim() !== '') {
              if (!contains(mixin['@type'], 'dcmls:CountryLanguageSpecific_0')) {
                 // NB. for now actually it is already there by default,
                 // TODO LATER rather optional / candidate mixin (FR/IT...)CountryLanguageSpecific
                 mixin['@type'].push('dcmls:CountryLanguageSpecific_0');
              }
              mixin['dcmls:code'] = specificToCountryLanguage.trim();
           }
              
           // TODO any other DCModel field
           

           var fieldPath = fieldRow["Field name"];
           if (typeof fieldPath !== 'undefined' && fieldPath !== null && fieldPath.trim().length !== 0) {
           
           var fieldPathElements = fieldPath.split("."); // at least one
           
           // filling import plan :
           var rootMixinName = mixinName;
           var fieldNameTreeCur = mixinNameToFieldNameTree[rootMixinName];
           if (typeof fieldNameTreeCur === 'undefined') {
              fieldNameTreeCur = {};
              mixinNameToFieldNameTree[rootMixinName] = fieldNameTreeCur;
           }
           var field = null;
           var subFieldType = null;
           var subFieldOrListField = null;
           var subFieldOrListFieldType = null;
           var candidateMixin;
           for (var i = 0; i < fieldPathElements.length - 1 ; i++) {
              var fieldName = fieldPathElements[i];

              if (!((subFieldType === 'list' || subFieldType ===  'i18n') && subFieldOrListFieldType === 'map' && isInteger(fieldName))) {
              
              if (fieldName.indexOf(":") == -1) {
                 var fieldMixinPrefix = mixin["dcmo:name"].replace(":", "_").replace("_" + importState.mixinMajorVersion, ""); // TODO better domainPrefix + "_" + mixinShortName; // NO . else KO in query (and tree path parsing)
                 fieldName = fieldMixinPrefix + ":" + fieldName; // buildFieldName()
              }
              // TODO fieldName from this short name
              // following the link in the model :
              subFieldOrListField = mixinFields[fieldName];
              if (typeof subFieldOrListField === 'undefined') {
                 // TODO error field must be defined in first pass NOO TODO TODO TODO TODO
                 importState.model.errors.push({ code : "missingSubFields", mixin : rootMixinName, field : fieldPath,
                       intermediateMixin : mixin, subFieldOrListField : fieldName }); // setting up another loop on fields
                 continue fieldsLoop;
              }
              
              subFieldType = importState.typeMap[subFieldOrListField["dcmf:type"].toLowerCase()];
              subFieldOrListFieldType = subFieldType;
              while (subFieldOrListFieldType === 'list' || subFieldOrListFieldType === 'i18n') {
                 // skip list (of list...)
                 subFieldOrListField = subFieldOrListField['dcmf:listElementField'];
                 subFieldOrListFieldType = importState.typeMap[subFieldOrListField["dcmf:type"].toLowerCase()];
              }
              
              if (subFieldOrListFieldType === 'map') {
                 mixinFields = subFieldOrListField["dcmf:mapFields"];
                 if (typeof mixinFields === 'undefined') {
                    mixinFields = {};
                    subFieldOrListField["dcmf:mapFields"] = mixinFields;
                 }
              } else if (subFieldOrListFieldType === 'resource') {
                 var subFieldResourceType = subFieldOrListField["dcmf:resourceType"];
                 candidateMixin = mixins[subFieldResourceType];
                 if (typeof candidateMixin === 'undefined') {
                    // TODO error mixin must be defined in first pass NOO TODO TODO TODO TODO
                    importState.model.errors.push({ code : "missingReferencedMixins", mixin : rootMixinName,
                          field : fieldPath, intermediateMixin : mixin, subfield : fieldName,
                          resourceType : subFieldResourceType }); // setting up another loop on fields
                    continue fieldsLoop;
                 }
                 mixin = candidateMixin;
                 mixinFields = mixin["dcmo:fields"];
              } else if (typeof subFieldType !== 'undefined') {
                 importState.model.errors.push({ code : "notMapOrResourceTypedFieldBeforeEndOfDottedPath", mixin : rootMixinName,
                       field : fieldPath, intermediateMixin : mixin, subField : fieldName,
                       subFieldType : subFieldOrListFieldType }); // setting up another loop on fields
                 continue fieldsLoop;
              }
              
              }
              
              // following the link in the import plan :
              var subFieldNameTreeCur = fieldNameTreeCur[fieldName];
              if (typeof subFieldNameTreeCur !== 'object' || subFieldNameTreeCur == null) { // null meaning resource field or first pass happened
                 subFieldNameTreeCur = {};
                 fieldNameTreeCur[fieldName] = subFieldNameTreeCur;
              }
              subFieldNameTreeCur['type'] = 'node';
              fieldNameTreeCur = subFieldNameTreeCur;
           }
           
           // last field path element :
           var fieldName = fieldPathElements[fieldPathElements.length - 1];
           
           if (!((subFieldType === 'list' || subFieldType ===  'i18n') && subFieldOrListFieldType === 'map' && isInteger(fieldName))) {
           
           if (fieldName.indexOf(":") == -1 && subFieldType !== 'i18n') {
              var fieldMixinPrefix = mixin["dcmo:name"].replace(":", "_").replace("_" + importState.mixinMajorVersion, ""); // TODO better ; NO "." else KO in query (and tree path parsing)
              fieldName = fieldMixinPrefix + ":" + fieldName; // buildFieldName()
           }
           // will be filled once field is built
           
              field = importField(fieldRow, fieldName, mixinFields, mixinTypeName, importState);
           } // else can be skipped, map listElementField already defined

           // import plan-specific (not in server-side model nor generic field import conf) :
           if (typeof fieldNameTreeCur[fieldName] === 'undefined') {
              fieldNameTreeCur[fieldName] = { type:'leaf' };
           }
           if (fieldNameTreeCur[fieldName]['type'] === 'leaf') { // NB. merge even if already seen & defined
              mergeImportConfStringValue(fieldNameTreeCur[fieldName], 'importconf:internalName',
                    fieldRow["Internal field name"].trim(), // else can't find ex. OpenElec "code_departement_naissance " !!
                    "string");
              mergeImportConfStringValue(fieldNameTreeCur[fieldName], 'importconf:defaultStringValue',
                    fieldRow["defaultValue"], "string");
              var fieldInternalName = fieldNameTreeCur[fieldName]['importconf:internalName'];
              if (fieldInternalName !== null) {
                 importState.model.fieldNamesWithInternalNameMap[fieldName] = null; // used as a set, to build importedFieldNames
              }
              if (!importState.model.fieldInternalNameToMixinNames[fieldInternalName]) {
                  importState.model.fieldInternalNameToMixinNames[fieldInternalName] = {};
              }
              importState.model.fieldInternalNameToMixinNames[fieldInternalName][mixin['dcmo:name']] = null; // to filter importableMixins (NB. several ones occur because of a dotted path to ex. geoco:idIso which can have geocofr:idIso as fieldInternalName)
           } // else may have already been seen within fieldPath
           
           } // end import field if any
           
           // TODO also mixins, resource links & sub...
        } // end line loop

         importState.model["loops"] = loopIndex + 1;
        
      } while (importState.model.errors.length != 0);
        
        // CUSTOM or import models (and not fields)
        // TODO OR RATHER binding than script ??!
        //mixins["elec:Canton_0"]["dcmo:idGenJs"] = "";
        //mixins["elec:ElectoralList_0"]["dcmo:idGenJs"] = "";
        //mixins["elec:Elector_0"]["dcmo:idGenJs"] = "'fr' + '/' + r['elec:City_0:CityINSEECode'] + '/' + r['elec_Elector:ElectorNumberInTheCity']";
        //mixins["elec:PollingStation_0"]["dcmo:idGenJs"] = "r['elec_PollingStation:PollingStationID']";
        //mixins["elec:Street_0"]["dcmo:idGenJs"] = ""; // NOOO mixin ; TODO id (?)
        /// TODO if (typeof mixins["elec:City_0"] !== 'undefined') mixins["elec:City_0"]["dcmo:idGenJs"] = "'/' + r['elec_City:INSEECode'] + '/' + r['elec_City:Name']";
        //mixins["elec:Department_0"]["dcmo:idGenJs"] = ""; // link only
        /// TODO if (typeof mixins["elec:Country_0"] !== 'undefined') mixins["elec:Country_0"]["dcmo:idGenJs"] = "'fr'"; // NOOO foreigners
        
        // making arrays of fields, fieldAndMixins, mixins :
        var modelArray = [];
        for (var mInd in mixins) {
            var mixin = mixins[mInd];
            
            // preparing identification rules :
            var idIndexToFieldNames = {};
            var parentIndexToFieldNames = {};
            var lookupQueryNameToFields = {};
            for (var idFieldName in mixin["dcmo:fields"]) {
               var idField = mixin["dcmo:fields"][idFieldName];
               ///fillIndexToEncodedValue(idField, resource, mixin, mixinMustBeImported, pathInFieldNameTree, importState);
               var indexInId = idField["dcmfid:indexInId"]; // NB. this import-specific conf has been enriched in refreshed involvedMixins' global fields
               if (typeof indexInId === 'number') {
                  idIndexToFieldNames[indexInId] = idFieldName;
                  if (mixin['dcmoid:useIdForParent'] === true && idField['dcmf:type'] === 'resource') {
                     parentIndexToFieldNames[indexInId] = idFieldName;
                  }
               }
               var indexInParents = idField["dcmfid:indexInParents"]; // NB. this import-specific conf has been enriched in refreshed involvedMixins' global fields
               if (typeof indexInParents === 'number') {
                  parentIndexToFieldNames[indexInParents] = idFieldName;
               }
               var queryNames = idField["dcmfid:queryNames"]; // NB. this import-specific conf has been enriched in refreshed involvedMixins' global fields
               if (queryNames) {
                  for (var qnInd in queryNames) {
                     var queryName = queryNames[qnInd];
                     if (typeof lookupQueryNameToFields[queryName] === 'undefined') {
                        lookupQueryNameToFields[queryName] = [];
                     }
                     lookupQueryNameToFields[queryName].push(idFieldName);
                  }
               }
            }
            var idFieldNb = Object.keys(idIndexToFieldNames).length;
            if (idFieldNb !== 0) {
               mixin['dcmoid:idFieldNames'] = [];
               for (var fInd = 0; fInd < idFieldNb; fInd++) {
                  mixin['dcmoid:idFieldNames'].push(idIndexToFieldNames[fInd]);
               }
            }
            var parentsFieldNb = Object.keys(parentIndexToFieldNames).length;
            if (parentsFieldNb !== 0) {
               mixin['dcmoid:parentFieldNames'] = [];
               for (var fInd = 0; fInd < parentsFieldNb; fInd++) {
                  mixin['dcmoid:parentFieldNames'].push(parentIndexToFieldNames[fInd]);
               }
            }
            var lookupQueryNb = Object.keys(lookupQueryNameToFields).length;
            if (lookupQueryNb !== 0) {
               mixin['dcmoid:lookupQueries'] = [];
               for (var lqName in lookupQueryNameToFields) {
                  mixin['dcmoid:lookupQueries'].push({
                     'dcmoidlq:name' : lqName,
                     'dcmoidlq:fieldNames' : lookupQueryNameToFields[lqName]
                  });
                  /*var lookupQueryIndexToFieldNames = lookupQueryNameToFields[qInd];
                  var lookupQueryFieldNames = [];
                  mixin['dcmid:lookupQueries'].push(lookupQueryFieldNames);
                  for (var fInd = 0; fInd < lookupQueryIndexToFieldNames.length; fInd++) {
                     lookupQueryFieldNames.push(lookupQueryIndexToFieldNames[fInd]);
                  }*/
               }
            }
            
            // copy mixin to array element :
            var modelArrayElt = {};
            for (var key in mixin) {
               if (key.lastIndexOf("importconf:", 0) === 0) { // startsWith
                  continue; // skip import-only fields that are not defined in server
               }
               modelArrayElt[key] = mixin[key];
            }
            modelArray.push(modelArrayElt);
            
            var fieldAndMixinNames = []; // fieldAndMixins (orders their overrides)
            for (var mKey in mixin["dcmo:mixins"]) {
               var mixinName = mixin["dcmo:mixins"][mKey];
               fieldAndMixinNames.push(mixinName);
            }
            var fieldArray = [];
            fieldsToArray(mixin["dcmo:fields"], fieldArray);
            var fields = mixin["dcmo:fields"];
            modelArrayElt["dcmo:fields"] = fieldArray;
            for (var fInd in fieldArray) {
               fieldAndMixinNames.push(fieldArray[fInd]['dcmf:name']);
            }
            // TOOO LATER also mixins in fieldAndMixinNames
            modelArrayElt["dcmo:fieldAndMixins"] = fieldAndMixinNames;
            mixin["dcmo:fieldAndMixins"] = fieldAndMixinNames; // enriching
        }
        importState.model["modelArray"] = modelArray;
        
        displayImportedModels(importState);

      callback(importState);
   }
   function displayImportedModels() {
      ///var results = eval('[' + data.content.data + ']')[0];
      //var prettyJson = toolifyDcResource(results, 0);
      //var mixinsPrettyJson = JSON.stringify(modelArray, null, '\t').replace(/\n/g, '<br>');
      // TODO LATER getPartial ?
      var mixinsPrettyJson = toolifyDcList(importState.model.modelArray, 0, null, 'dcmo:model_0');
      $('.importedResourcesFromCsv').html(mixinsPrettyJson);
   }
   
   function fieldsToArray(fields, fieldArray) {
      for (var fieldName in fields) {
         var field = fields[fieldName];
         // copy field to clean it up from import conf :
         var cleanField = fieldToCleanField(field);
         fieldArray.push(cleanField);
         ///fieldAndMixinNames.push(fieldName);
      }
   }
   function fieldToCleanField(field) {
      // copy field to clean it up from import conf :
      var cleanField = {};
      for (var key in field) {
         if (key.lastIndexOf("importconf:", 0) === 0) { // startsWith
            continue; // skip import-only fields that are not defined in server
         }
         cleanField[key] = field[key];
      }
      if (cleanField['dcmf:type'] === 'map') {
         cleanField['dcmf:mapFields'] = [];
         fieldsToArray(field['dcmf:mapFields'], cleanField['dcmf:mapFields']);
      } else if (cleanField['dcmf:type'] === 'list' || cleanField['dcmf:type'] === 'i18n') {
         cleanField['dcmf:listElementField'] = fieldToCleanField(field['dcmf:listElementField']);
      }
      return cleanField;
   }
   
   // only for models
   function refreshAndSchedulePost(modelOrMixinArray, relativeTypeUrl, postedCallback, importState) {
      function refreshAndPostObsoleteUntilFresh(data, origResource) {
         if (data._raw.statusCode === 409) {
            // refresh then repost :
            if (origResource instanceof Array) { // because arg of postAllDataInType()
                origResource = origResource[0];
            }
            getData(origResource['@id'], function (resource) {
               // updating existing resource : 
               // NB. can't access "mixin" variable because has been changed since call is async
               /*var upToDateMixin = findMixin(resource["dcmo:name"], modelOrMixinArray);
               upToDateMixin["o:version"] = resource["o:version"];*/
               origResource["o:version"] = resource["o:version"];
               postAllDataInType(data.request.path, origResource,
                     postedCallback, refreshAndPostObsoleteUntilFresh);
            }, postedCallback);
            
         } else if (data._raw.statusCode === 403
               && skipForbidden(origResource, importState.model.posted, importState)) { // skip model
            postedCallback(null, origResource);
            
         } else {
            postedCallback(data, origResource); // can't handle error, pass it along
         }
      }
      
      for (var mInd in modelOrMixinArray) {
         if (importState.aborted) {
            return abortImport();
         }
         var mixin = modelOrMixinArray[mInd];
         var uri = mixin["@id"];

         // skip model already here (models will anyway be refreshed in query on parsed fields)
         if (skipModel(mixin, importState.model.posted, importState)) {
            continue;
         }
         
         // posting one at a time rather than all at once because version has
         // to be refreshed and it is easier to do it in sync this way
         getData(uri, function (resource, relativeUrl, data, importState) {
            // existing resource...
            // NB. can't access "mixin" variable because has been changed since call is async
            var upToDateMixin = findMixin(resource["dcmo:name"], modelOrMixinArray);
            // updating version :
            upToDateMixin["o:version"] = resource["o:version"];
            // should we skip posting ?
            if (skipProject(resource, importState.model.posted, importState)) {
               // not same project : skip TODO better log & only if security constraint
               /*importedDataPosted({ _raw : { statusCode : 403 }, _headers:{}, _body:{ _body:
                   'Forbidden to post resource ' + resource["@id"] + ' whose model is in project '
                   + resourceModel['dcmo:pointOfViewAbsoluteName'] + ' that is not current one' } }, upToDateMixin);*/
               postedCallback(null, resource); // go on (null data because error already handled)
               return;
            }
            // TODO LATER OPT check project again, in case returnedResource has a different modelType ???
            postAllDataInType(relativeTypeUrl, upToDateMixin,
                  postedCallback, refreshAndPostObsoleteUntilFresh);
         }, function (data, relativeUrl, importState) { // error because resource does not exist
            // new resource...
            var relativeUrl = data.request.path;
            var resourceIri = relativeUrl.substring(relativeUrl.indexOf("/dc/type/") + "/dc/type/".length);
            var typeName = decodeURIComponent(resourceIri.substring(resourceIri.indexOf("/") + 1)); // else "elec%3ADepartment_0" ; AND NOT decodeURI
            var upToDateMixin = findMixin(typeName, modelOrMixinArray);
            // checking error first (rights...) :
            if (data._raw.statusCode !== 404) { // TODO more specific 403 Forbidden ?
               postedCallback(data, upToDateMixin); // pass error upwards
               return;
            }
            // TODO better skipped error
            // so creating :
            postAllDataInType(relativeTypeUrl, upToDateMixin,
                  postedCallback, postedCallback);
         }, null, importState);
      }
   }

   function loadConf(importStateConf, importState) {
      importState.domainPrefix = importStateConf.domainPrefix; // changed by UI on model file select to its first three letters, else elec
      
      // model :
      if (typeof importStateConf.model.rowLimit === 'number') { // && importStateConf.model.rowLimit < 500
         importState.model.rowLimit = importStateConf.model.rowLimit;
      } // else use default
      if (typeof importStateConf.model.fromMixin === 'string' && importStateConf.model.fromMixin.length !== 0) {
         importState.model.fromMixin = importStateConf.model.fromMixin;
      } // else use default
      if (typeof importStateConf.model.untilMixin === 'string' && importStateConf.model.untilMixin.length !== 0) {
         importState.model.untilMixin = importStateConf.model.untilMixin;
      } // else use default
      if (typeof importStateConf.model.posted.skipForbidden !== 'undefined') {
          importState.model.posted.skipForbidden = importStateConf.model.posted.skipForbidden;
      }
      // building importedMixinNameSet out of it :
      if (importStateConf.model.mixinNames) {
         importState.model.mixinNames = importStateConf.model.mixinNames;
        
         
         var beforeFirst = true;
         for (var i in importState.model.mixinNames) {
        	
        	 
        	 if (beforeFirst) {
               if (importState.model.mixinNames[i] !== importState.model.fromMixin) {
                  continue;
               }
               beforeFirst = false;
            }
          
            
            if (importState.model.mixinNames[i] === importState.model.untilMixin) {
               break;
            }            
            importState.model.importedMixinNameSet[buildMixinName(importState.model.mixinNames[i], importState)] = true;
         }
      }
      
      if (importStateConf.model.fileName && importStateConf.model.fileName !== "") {
         importState.model.fileName = importStateConf.model.fileName;
         importState.model.file = importStateConf.model.file;
      } // else use default
      
      // resource :
      if (typeof importStateConf.data.rowLimit === 'number') { // && importStateConf.data.rowLimit < 500
         importState.data.rowLimit = importStateConf.data.rowLimit;
      } // else use default

      if (typeof importStateConf.data.rowStart === 'number' && importStateConf.data.rowStart > 0) { // && importStateConf.data.rowStart < 50000
         importState.data.rowStart = importStateConf.data.rowStart;
      } // else use default
      
      importState.data.detailedErrors = importStateConf.data.detailedErrors;
      
      if (importStateConf.data.fileName && importStateConf.data.fileName !== "") {
         importState.data.fileName = importStateConf.data.fileName;
         importState.data.file = importStateConf.data.file;
      } // else use default
   }
   
   function resetResourceCounters() {
      $('.resourceRowCounter').html("Handled no resource row yet");
      $('.resourceCounter').html("Posted no resource yet");
   }
   function resetModelCounters() {
      $('.modelRowCounter').html("Handled no model row yet");
      $('.modelCounter').html("Posted no model yet");
   }
   /*function abortImport(msg) {
      if (msg) {
         msg = " Aborted. " + msg;
      } else {
         msg = " Aborted."
      }
      $('.resourceRowCounter').html($('.resourceRowCounter').html() + msg);
      $('.importedResourcesFromCsv').html(msg);
      throw msg;
   }*/
   function abortImport(msg) {
      if (!window.importState) {
         return false; // already called
      }
      
      var importState = window.importState;
      if (importState.data.posted.postedNb !== 0) {
         displayImportedResourcesPosted(importState.data.posted);
      } else if (importState.data.rowNb !== 0) {
         displayParsedResource(importState);
      } else if (importState.model.posted.postedNb !== 0) {
         displayImportedResourcesPosted(importState.model.posted);
      } else if (importState.model.modelArray !== null) {
         displayImportedModels(importState);
      }
      
      if (msg) {
         console.log('Aborted', msg);
         if (msg instanceof Error) {
            msg = "Aborted. " + msg.message + " at " + msg.fileName + " " + msg.lineNumber + ":" + msg.columnNumber + ". ";
            ///throw msg; // to get the error line in the debugger
         } else {
            msg = "Aborted. " + msg + ". ";
         }
      } else {
         msg = "Aborted. "
      }
      $('.resourceRowCounter').html(msg + $('.resourceRowCounter').html());
      $('.importedResourcesFromCsv').html(msg);
      
      concludeImport();
      return true;
   }
   function concludeImport() {
      delete window.importState;
      $('.dc-import-button').html('<b>import</b>');
   }
   function importModelAndResources(importStateConf) {
      if (window.importState) {
         window.importState.aborted = true;
         $('.dc-import-button').html('aborted');
         //abortImport(); // not only in loops, also there so that it's sure to happen
         return;
      }
      
      resetModelCounters();
      resetResourceCounters();
      
      var importState = {
            aborted : false,
            // CUSTOM
            typeMap : { // TODO more
                  "string" : "string",
                  "integer" : "int", // alias
                  "int" : "int",
                  "long" : "long",
                  "float" : "float",
                  "double" : "double",
                  "boolean" : "boolean",
                  "date" : "date",
                  "list" : "list",
                  "map" : "map",
                  "resource" : "resource",
                  "i18n" : "i18n"
            },
            domainPrefix : 'elec', // first three letters of model import file, changed on file select by UI calling buildModelDomainPrefix()
            project : getProject(),
            mixinMajorVersion : 0, // TODO better
            metamodel : {},
            metamodelProject : 'oasis.meta',
            ///metamodelPrefix : 'dcm', // dcmo:model_0, dcmi:mixin_0...
            model : {
               // conf :
               domainPrefix : 'elec',
               rowLimit : -1,
               fromMixin : null,
               untilMixin : null,
               mixinNames : [], // not normalized (no : prefix), fills from/untilMixin selects
               importedMixinNameSet : {}, // normalized names (: prefix), used as set, built from mixinNames & from/untilMixin
               // state :
               file : null,
               fileName : 'samples/openelec/oasis-donnees-metiers-openelec.csv',
               defaultRow : {},
               modelOrMixins : {}, // NOOOO MUST NOT BE USED outside of csvToModel because have no global fields, rather use .data.involvedMixins
               mixinNameToFieldNameTree : {},
               fieldNamesWithInternalNameMap : {}, // used as set to get all models or mixins that are imported
               fieldInternalNameToMixinNames : {}, // to filter importableMixins
               ///modelOrMixinArray : null, // MUST NOT BE USED outside of csvToModel because have no global fields, rather use .data.involvedMixins
               ///mixinArray : null, // MUST NOT BE USED outside of csvToModel because have no global fields, rather use .data.involvedMixins
               modelArray : null, // MUST NOT BE USED outside of csvToModel because have no global fields, rather use .data.involvedMixins
               loops : 0,
               warnings : [],
               errors : [],
               posted : {
                  skipForbidden : false,
                  startTime : null, // moment()
                  endTime : null, // moment()
                  errors : [], // not used yet
                  warnings : [], // not used yet
                  toBePostedNb : 0, // = resources nb
                  postedResources : {},
                  postedNb : 0,
                  sentResourceUriSet : {}, // used as set
                  sentNb : 0,
                  skippedResourceUris : [], // not a set to allow only displaying the first ones
                  skippedNb : 0,
                  skippedProjectSet : {}, // used as set
                  skippedModelTypeSet : {}, // used as set
                  postedResourceUris : [], // to have their order (NOO already in postedResources)
                  postedSuccessNb : 0, // computed from postedResources
                  postedErrors : {},
                  postedErrorNb : 0,
                  errorHtml : ""
               }
            },
            data : {
               // conf :
               rowLimit : 50,
               rowStart : 0,
               // state :
               startTime : null, // moment()
               endTime : null, // moment()
               file : null,
               fileName : 'samples/openelec/electeur_v26010_sample.csv',
               columnNames : null,
               involvedMixins : {}, // name to mixin map
               importableMixins : {}, // name to mixin map
               rows : null,
               rowNb : 0,
               rInd : 0, // current row number, not in pull parser mode
               row : null, // current row
               /*row : { // current row
                  loopIndex : 0,
                  resourceRow : null,
                  blockedModelTypeSet : {},
                  modelTypeToRowResources : {},
                  fieldNameTreeStack : [],
                  pathInFieldNameTree : [], // for logging only
                  iteration : {
                        missingIdFieldMixinToResources : {},
                        errors : [],
                        warnings : []
                  },
                  previousMissingIdFieldResourceOrMixinNb : -1,
                  missingIdFieldResourceOrMixinNb : -1,
                  lookupQueriesToRun : [], // of URIs, LATER rather of { modelType : '', query : buildUriQuery({ 'fieldName' : 'operatorValueSort }) }
                  done : false
               },*/
               lookupQueryToResource : {},
               cachedResources : {},
               resources : {},
               warnings : [],
               errors : [],
               detailedErrors : true, // log error contains modelTypeToRowResources
               posted : {
                  startTime : null, // moment()
                  endTime : null, // moment()
                  errors : [], // not used yet
                  warnings : [], // not used yet
                  toBePostedNb : 0, // = resources nb
                  postedResources : {},
                  postedNb : 0,
                  sentResourceUriSet : {}, // used as set
                  sentNb : 0,
                  skippedResourceUris : [], // not a set to allow only displaying the first ones
                  skippedNb : 0,
                  skippedProjectSet : {}, // used as set
                  skippedModelTypeSet : {}, // used as set
                  postedResourceUris : [], // to have their order (NOO already in postedResources)
                  postedSuccessNb : 0, // computed from postedResources
                  postedErrors : {},
                  postedErrorNb : 0,
                  errorHtml : ""
               }
            }
      }


      // load custom conf if any :
      if (importStateConf) {
         loadConf(importStateConf, importState);
      } // else using defaults
      
      // do checks before setting up start :
      if (!importState.model.file && importState.project !== 'oasis.sandbox') {
         alert('Please choose a model file !');
         return;
      }

      // setting up start :
      window.importState = importState; // making available globally for abort
      $('.dc-import-button').html('abort');
      
      
      // loading existing metamodel : 
      findDataByType({ modelType : 'dcmo:model_0' }, function (resources, relativeUrl, data, importState) {
      /*findDataByType({ modelType : 'dcmo:model_0', query : new UriQuery(
         'dcmo:name', '$regex' + importState.metamodelPrefix + '.*'
      ).s() }, function (resources) {*/
         for (var mmInd in resources) {
            var modelResource = resources[mmInd];
            importState["metamodel"][modelResource["dcmo:name"]] = modelResource;
         }

         
      var modelParsingConf = {
         download: true,
         header: true,
         comments: true, // skip # or // starting lines
         preview: importState.model.rowLimit, // ex. '1' user input means importing
         // title line + model default line + 1 more (field) line
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
                  return abortImport(importState.model.errors.length + ' model parsing errors');
               }
               
               function fillDataWhenAllModelsUpdated(resourcesOrData, origResources) {
                  importedResourcePosted(resourcesOrData, importState.model.posted, importState,
                        "models or mixin", $('.modelCounter'), origResources, fillData, concludeImport);
               };

               importState.model.posted.startTime = moment();
               importState.model.posted.toBePostedNb = importState.model.modelArray.length;
               refreshAndSchedulePost(importState.model.modelArray, { modelType : 'dcmo:model_0' },
                     fillDataWhenAllModelsUpdated, importState);
            });
         }
      };
      
      // starting parsing :
      if (importState.model.file) {
         importState.model.file.parse({ config : modelParsingConf });
      } else {
         // file must be online at this relative location (case of default file)
         Papa.parse(importState.model.fileName + "?reload="
               + new Date().getTime(), modelParsingConf); // to prevent browser caching
      }

      }, null, null, 100, // max limit
      { 'X-Datacore-Project' : importState.metamodelProject }, importState); // everything in metamodel project
      return false;
   }
