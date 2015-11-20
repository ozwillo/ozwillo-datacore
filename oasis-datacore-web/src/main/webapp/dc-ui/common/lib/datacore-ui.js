//var containerUrl = "http://data.ozwillo.com/"; // rather in dcConf filled at init by /dc/playground/configuration


//////////////////////////////////////////////////:
// URI
   

   // ALSO IN swagger.js SO IT IS SELF-CONTAINED :

   // in swagger.js SwaggerOperation.prototype.encodePathParam(pathParam) rather than encodeUriPath(uriPath) :
   function encodeUriPath(pathParam) { 
      /*if (uriPath.indexOf("/") === -1) {
         return encodeUriPathComponent(uriPath);
      }
      var slashSplitPath = uriPath.split('/');
      for (var pathCptInd in slashSplitPath) {
         slashSplitPath[pathCptInd] = encodeUriPathComponent(slashSplitPath[pathCptInd]); // encoding ! NOT encodeURIComponent
      }
      return slashSplitPath.join('/');
      //return encodeURI(idValue); // encoding !
      // (rather than encodeURIComponent which would not accept unencoded slashes)*/
       var encParts, part, parts, _i, _len;
       if (pathParam.indexOf("/") === -1) {
         return encodeUriPathComponent(pathParam);
       } else if (pathParam.indexOf("//") !== -1) { // else '//' (in path param that is ex. itself an URI)
         // would be equivalent to '/' in URI semantics, so to avoid that encode also '/' instead
         return encodeURIComponent(pathParam);
       } else {
         parts = pathParam.split("/");
         encParts = [];
         for (_i = 0, _len = parts.length; _i < _len; _i++) {
           part = parts[_i];
           ///encParts.push(encodeURIComponent(part));
           encParts.push(encodeUriPathComponent(part));
         }
         return encParts.join("/");
       }
   }

   var safeCharsRegexString = "0-9a-zA-Z" + "\\$\\-_\\.\\+!\\*'\\(\\)"; // "$-_.()" + "+!*'";
   var reservedCharsRegexString = "$&+,/:;=@" + "~"; // NOT ? and besides ~ is not encoded by Java URI
   var pathComponentSafeCharsRegex = new RegExp('[' + safeCharsRegexString + reservedCharsRegexString + ']');
   function encodeUriPathComponent(pathCpt) {
      var res = '';
      for (var cInd in pathCpt) {
         var c = pathCpt[cInd];
         res += pathComponentSafeCharsRegex.test(c) ? c : encodeURIComponent(c);
      }
      return res;
   }
   
   
   // OTHER STUFF :
   
   function decodeIdSaveIfNot(idValue, idField) {
      if (typeof idField !== 'undefined' && idField !== null) {
         var dontDecode = idField["importconf:dontEncodeIdInUri"];
         if (dontDecode) {
            return idValue;
         }
      }
      var slashSplitId = idValue.split('/');
      for (var idCptInd in slashSplitId) {
         slashSplitId[idCptInd] = decodeURIComponent(slashSplitId[idCptInd]); // decoding !
      }
      return slashSplitId.join('/');
      //return decodeURI(idValue); // decoding !
      // (rather than decodeURIComponent which would not accept unencoded slashes)
   }
   function encodeIdSaveIfNot(idValue, idField) {
      if (typeof idField !== 'undefined' && idField !== null) {
         var dontEncode = idField["importconf:dontEncodeIdInUri"];
         if (dontEncode) {
            return idValue;
         }
      }
      if (idValue.indexOf('//') === -1) {
         return encodeUriPath(idValue);
      }
      // else '//' (in id value that is ex. itself an URI) would be equivalent
      // to '/' in URI semantics, so to avoid that encode also '/' instead
      return encodeURIComponent(idValue);
   }
   function buildUri(typeName, id,
         shouldEncodeId) { // optional, default is false
      return dcConf.containerUrl + buildRelativeUrl(typeName, id, shouldEncodeId);
   }
   function buildRelativeUrl(typeName, id,
         shouldEncodeId) { // optional, default is false
      // encoding ! NOT encodeURIComponent
      if (typeof id === 'undefined') {
         return "/dc/type/" + encodeUriPathComponent(typeName);
      }
      return "/dc/type/" + encodeUriPathComponent(typeName)
            + "/" + (shouldEncodeId ? encodeIdSaveIfNot(id) : id);
   }
   function identity(o) {
      return o;
   }
   // supports multi criteria ex. /dc/type/pli:city_0?dc:modified=>=2013-11-30T11:14:42.528+01:00&dc:modified=<=2016-11-30T11:14:42.528+01:00
   function buildUriQuery(queryFieldNameToOperatorValues, dontEncode) {
      var encodeOrNot = (dontEncode) ? identity : encodeURIComponent;
      var query = null;
      for (var queryFieldName in queryFieldNameToOperatorValues) {
         var operatorValues = queryFieldNameToOperatorValues[queryFieldName];
         var operatorValuesNb = operatorValues.length;
         if (operatorValuesNb === 0) {
            continue;
         } // else at least one
         if (query === null) { // first time
            query = '';
         } else {
            query += '&';
         }
         for (var vInd = 0; vInd < operatorValuesNb ; vInd++) {
            if (vInd !== 0) {
               query += '&';
            }
            query += encodeOrNot(queryFieldName) + '=' + encodeOrNot(operatorValues[vInd]);
         }
      }
      return query;
   }
   // supports multi criteria ex. /dc/type/pli:city_0?dc:modified=>=2013-11-30T11:14:42.528+01:00&dc:modified=<=2016-11-30T11:14:42.528+01:00
   function UriQuery(param, value) {
      this.params = {};
      if (typeof value !== 'undefined') {
         this.p(param, value);
      }
   }
   UriQuery.prototype.p = function (param, value) {
      if (typeof this.params[param] === 'undefined') {
         this.params[param] = [];
      }
      this.params[param].push(value);
      return this;
   }
   UriQuery.prototype.s = function() {
      return buildUriQuery(this.params);
   }
   // assume arg is a query (and not a query param), meaning it can't be encoded by swagger.js
   // (because it wouldn't know which '&' and '=' not to encode), so allow to encode it
   // at UI level using parseUriQuery() and buildUriQuery() :
   function parseUriQuery(queryString, dontDecode) {
      var query = {};
      var queryElts = queryString.split('&');
      for (var qeInd in queryElts) {
         var queryElt = queryElts[qeInd];
         var equalIndex = queryElt.indexOf('=');
         if (equalIndex === -1) {
            continue;
         }
         var queryEltParam = queryElt.substring(0, equalIndex);
         var queryEltValue = queryElt.substring(equalIndex + 1, queryElt.length);
         if (!dontDecode) {
            queryEltParam = decodeURIComponent(queryEltParam);
            queryEltValue = decodeURIComponent(queryEltValue);
         }
         if (typeof query[queryEltParam] === 'undefined') {
            // support multi criteria ex. /dc/type/pli:city_0?dc:modified=>=2013-11-30T11:14:42.528+01:00&dc:modified=<=2016-11-30T11:14:42.528+01:00
            query[queryEltParam] = [];
         }
         query[queryEltParam].push(queryEltValue);
      }
      return query;
   }
   // only kept for backward compatibility
   // only supports relative uri (iri) ex. /dc/type/model/id and query, but modelType at least is required
   function parseIri(resourceIri) {
      return parseIri(resourceIri);
   }
   // resourceUri must be well encoded URI
   // (otherwise, such as in UTF-8-written URIs in playground & samples, encode ex. encodeUri("http://..."))
   // also supports relative uri (iri) and query, but modelType is required
   //var dcResourceUriRegex = /^http:\/\/data\.ozwillo\.com\/dc\/type\/([^\/]+)\/(.+)$/g;
   //var dcResourceUriRegex = /^(http[s]?):\/\/+([^\/]+)\/+dc\/+type\/+([^\/\?]+)\/*([^\?]+)?\??(.*)$/g; // NOO seems stateful, else sometimes matches gets null
   //var dcResourceIriRegex = /^\/+dc\/+type\/+([^\/\?]+)\/*([^\?]+)?\??(.*)$/g; // NOO seems stateful, else sometimes matches gets null
   function parseUri(resourceUri) {
      var matchInd, matches, containerUrl;
      if (resourceUri.indexOf('http') !== 0) { // relative uri case
         if (resourceUri.indexOf('/dc/') !== 0) { // assuming modelType and optional query case
            resourceUri = '/dc/type/' + resourceUri;
         }
         matches = /^\/+dc\/+(type|h)\/+([^\/\?]+)\/*([^\?]+)?\??(.*)$/g.exec(resourceUri);
         containerUrl = dcConf.containerUrl;
         matchInd = 0;
      } else {
          matches = /^(http[s]?):\/\/+([^\/]+)\/+dc\/+(type|h)\/+([^\/\?]+)\/*([^\?]+)?\??(.*)$/g.exec(resourceUri);
          containerUrl = matches[1] + '://' + matches[2];
          matchInd = 2;
      }
      /*var pInd = resourceUri.indexOf('/dc/type/');
      var containerUrl = resourceUri.substring(0, pInd);
      var mInd = pInd + '/dc/type/'.length;
      var nextSlashInd = resourceUri.indexOf('/', mInd);
      var encModelType = resourceUri.substring(mInd, (nextSlashInd !== -1) ? nextSlashInd : resourceUri.length);
      var encId = resourceUri.substring(resourceUri.indexOf('/', mInd) + 1);
      if (containerUrl.length === 0) {
         containerUrl = dcConf.containerUrl;
         resourceUri = containerUrl + resourceUri;
      }
      return {
         containerUrl : containerUrl,
         modelType : decodeURIComponent(encModelType),
         id : decodeURI(encId),
         uri : resourceUri
         };*/
      
      var isHistory = matches[matchInd + 1] // else is a find
         && matches[matchInd + 1] === 'h';
      var encodedModelType = matches[matchInd + 2];
      var modelType = decodeURIComponent(encodedModelType); // required ; assuming that never contains %
      // NB. modelType encoded as URIs should be, BUT must be decoded before used as GET URI
      // because swagger.js re-encodes
      var encodedId = matches[matchInd + 3];
      var query = matches[matchInd + 4]; // no decoding, else would need to be first split along & and =
      var version = null;
      if (isHistory) {
          try {
            var versionSlashIndex = encodedId.lastIndexOf('/');
            version = parseInt(encodedId.substring(versionSlashIndex + 1));
            if (encodedId) {
               encodedId = encodedId.substring(0, versionSlashIndex);
            }
         } catch (e) {}
      }

      var uri = containerUrl + '/dc/type/' + encodedModelType;
      
      var id = null;
      if (encodedId) {
          id = decodeURIComponent(encodedId); // and not decodeURI else an URI won't be decoded at all
          // ex. "https%3A%2F%2Fwww.10thingstosee.com%2Fmedia%2Fphotos%2Ffrance-778943_HjRL4GM.jpg
          uri += '/' + encodedId;
      }
      if (!query) {
         query = null;
      }
      if (query !== null) {
         uri += '?' + query;
      }
      return {
         containerUrl : containerUrl,
         modelType : modelType, // decoded
         id : id,
         encodedId : encodedId,
         version : version, // only if isHistory
         query : query, // encoded
         uri : uri // resourceUri
         };
   }
   function getRelativeUrl(uri) {
      if (typeof uri === 'object') {
         uri = uri.uri;
      }
      var pInd = uri.indexOf("/dc/type/");
      if (pInd === -1) {
         throw "not an uri (nor path) : " + uri;
      }
      return uri.substring(pInd);
   }
   /*
   function getModelTypeAndIdUrlPart(uri) {
      if (typeof uri === 'object') {
         uri = uri.uri;
      }
      var pInd = uri.indexOf("/dc/type/");
      if (pInd === -1) {
         return uri;
      }
      return uri.substring(pInd + "/dc/type/".length);
   }
   */
   function getModelTypeUrl(uri) {
      if (typeof uri !== 'object') {
         uri = parseUri(uri);
      }
      return uri.containerUrl + "/dc/type/" + encodeUriPathComponent(uri.modelType);
      /*if (typeof uri === 'object') {
         uri = uri.uri;
      }
      var pInd = uri.indexOf("/dc/type/");
      if (pInd === -1) {
         return uri;
      }
      var mInd = pInd + "/dc/type/".length;
      var iInd = uri.indexOf('/', mInd);
      if (iInd === -1) {
         iInd = uri.length;
      }
      return uri.substring(pInd, iInd);*/
   }
   
   
///////////////////////////////////////////////////
// PLAYGROUND TOOLIFY
   
function lineBreak(depth) {
	var res = '\n'; // \n OR <br> but not both because in pre code
	for (var i = 0; i < depth; i++) {
		res += '   '; // or \t
	}
	return res;
}
// modelType is where queries are made in, therefore it's the upper resource's
// keyPathInResource is used to make queries in it
var skippedNativeFieldNames = {
      "@type" : null, "o:version" : null, "dc:creator" : null, "dc:contributor" : null
      // skip them because :
      // - query on a @type is the same as GET @type/...
      // - others are not indexed, and query on version is meaningless anyway 
      // BUT DONT SKIP @id (useful for iteration in range-based pagination),
      // dc:created (built in mongo _id), dc:modified (indexed)
}
function toolifyDcResourceFieldAndColon(value, key, modelType, upperResource, keyPathInResource) {
   if (typeof skippedNativeFieldNames[key] !== 'undefined' // skip native fields
      || typeof upperResource === 'undefined') { // skip when hashmap of resources
      return JSON.stringify(key, null, '\t') + " : ";
   }
   if (key === '@id') { 
      // iteration (for range-based pagination) example :
      return '"key"'
         + '<a href="' + buildRelativeUrl(modelType) + '?' + new UriQuery(keyPathInResource.join('.'), '>' + value + '+').s() + '" class="dclink dclinkGreater" onclick="'
         + 'javascript:return findDataByType($(this).attr(\'href\'));'
         + '"> : </a>';
   }
   // field model (TODO LATER find where it is first defined even if list or subfield) then equality query :
   ///return '"<a href="' + buildRelativeUrl('dcmo:model_0', modelType) + '" class="dclink dclinkType" onclick="'
   return '"<a href="' + '/dc/type/dcmo:model_0?' + new UriQuery('dcmo:fields.dcmo:name', key).s() + '" class="dclink dclinkType" onclick="'
      + 'javascript:return getData($(this).attr(\'href\'));'
      + '">' + key + '</a>"'
      + '<a href="' + buildRelativeUrl(modelType) + '?' + new UriQuery(keyPathInResource.join('.'), value).s() + '" class="dclink" onclick="'
      + 'javascript:return findDataByType($(this).attr(\'href\'));'
      + '"> : </a>';
}
// modelType, upperResource, keyPathInResource are not required for top level resource
function toolifyDcResource(resource, depth, modelType, upperResource, keyPathInResource) { // or map
   if (resource == null) {
      return 'null'; // in case of getData() (else done in ...Values)
   }
   if (typeof upperResource === 'undefined' || upperResource === null) {
      // top level resource
      if (resource["@type"] instanceof Array && resource["@type"].length != 0) {
         // resource
         modelType = resource["@type"][0];
         upperResource = resource;
      } else if (typeof resource["@id"] !== 'undefined') {
         modelType = parseUri(resource["@id"]).modelType;
         upperResource = resource;
      } // else mere hashmap of resources, use elements themselves as resources
      keyPathInResource = []; // else KO below
   } // else : map (keep upper modelType), or TODO LATER embedded subresource
   var res = '{';
   var first = true;
   var subDepth = depth + 1;
   for (var key in resource) {
	   if (first) {
		   first = false;
	   } else {
		   res += ',';
	   }
	   res += lineBreak(subDepth);
	   var value = resource[key];
	   //resource[key] = toolifyDcResourceValue(value, key, subDepth, modelType, resource, keyPathInResource);
	   try {
	      keyPathInResource.push(key);
         res += toolifyDcResourceFieldAndColon(value, key, modelType, upperResource, keyPathInResource)
               + toolifyDcResourceValue(value, key, subDepth, modelType, upperResource, keyPathInResource);
	   } catch (e) {
	      console.log('Error in toolify', upperResource, keyPathInResource, value, e);
      } finally {
	      keyPathInResource.pop();
	   }
   }
   if (!first) {
	   // at least one (should !)
	   res += lineBreak(depth);
   }
   res += '}';
   return res;
   //return resource;
}
// null values supported
function toolifyDcList(values, depth, key, modelType, upperResource, keyPathInResource) {
   if (values == null || values.length == 0) {
      return '[]';
   }
   var value;
   var res = '[';
   var first = true;
   var subDepth = depth + 1;
   for (var vInd in values) {
	   if (first) {
		   first = false;
	   } else {
		   res += ',';
	   }
	   res += lineBreak(subDepth);
	   value = values[vInd];
 	   res += toolifyDcResourceValue(value, key, subDepth, modelType, upperResource, keyPathInResource);
   }
   if (!first) {
	   // at least one
	   res += lineBreak(depth);
   }
   res += ']';
   return res;
   //return resource;
}
function toolifyDcResourceUri(value) {
   // TODO LATER rather encodeUriPathComponent('dcmo:model_0') etc. (however, $1 and $2 are already encoded)
   return '"' + value.replace(/^http:\/\/data\.ozwillo\.com\/dc\/type\/([^\/]+)\/(.+)$/g,
         dcConf.containerUrl + '/dc/'
         + '<a href="/dc/type/dcmo:model_0/$1" class="dclink dclinkType" onclick="'
         + 'javascript:return getData($(this).attr(\'href\'));'
         + '">type</a>'
         + '/'
         + '<a href="/dc/type/$1" class="dclink" onclick="'
         + 'javascript:return findDataByType($(this).attr(\'href\'));'
         + '">$1</a>'
         + '<a href="/dc/type/dcmo:model_0?dcmo:globalFields.dcmf:resourceType=$1" class="dclink" onclick="'
         + 'javascript:return findLinkedData(\'/dc/type/$1/$2\');'
         + '">/</a>'
         + '<a href="/dc/type/$1/$2" class="dclink" onclick="'
         + 'javascript:return getData($(this).attr(\'href\'));'
         + '">$2</a>') + '"';
}
function toolifyDcResourceValue(value, key, depth, modelType, upperResource, keyPathInResource) {
   if (value == null) {
      return 'null';
   }
	///if ("o:version" == key || "dc:created" == key || "dc:modified" == key) { // skip
	var valueType = (typeof value);
	if (valueType== 'string') {
		if ("@type" == key // in list
				|| "dcmf:resourceType" == key) { // for Models
			return '"<a href="' + buildRelativeUrl(value) + '" class="dclink dclinkType" onclick="'
				      + 'javascript:return findDataByType($(this).attr(\'href\'));'
	         + '">' + value + '</a>"';
		}
		///if ("@id" == key) {
		if (value.indexOf('http') !== 0) { // shortcut
		   return value;
		}
		return toolifyDcResourceUri(value);
	} else if (valueType == 'object') {
		if (value instanceof Array) {
			return toolifyDcList(value, depth, key, modelType, upperResource, keyPathInResource);
		} else { // map
	 	   return toolifyDcResource(value, depth, modelType, upperResource, keyPathInResource);
		}
	} // 'number', 'boolean'(?) : nothing to do ; TODO others (date) ??
	return JSON.stringify(value, null, '\t');
}
function toolifyDcListOrResource(valuesOrResource) {
   if (typeof valuesOrResource.length !== 'undefined') { // array (=== 'object not enough)
      return toolifyDcList(valuesOrResource, 0);
   } else {
      return toolifyDcResource(valuesOrResource, 0);
   }
}

function toolifyDcResourcePartial(resources, rowNb) {
   var partialRes = getPartial(resources, rowNb);
   return toolifyDcResource(partialRes.res, 0) + ((partialRes.isPartial) ? '<br/>...' : '');
}
function stringifyPartial(arrayOrHashmap, rowNb) {
   var partialRes = getPartial(arrayOrHashmap, rowNb);
   return JSON.stringify(partialRes.res, null, '\t').replace(/\n/g, '<br>')
         + ((partialRes.isPartial) ? '<br/>...' : '');
}
function stringifyForAttribute(headers) {
   return headers ? JSON.stringify(headers, null, null).replace(/"/g, "'") : null;
}


///////////////////////////////////////////////////
// PARTIAL

// returns { isPartial : isPartial, res : partialArrayOrHashmap } 
function getPartial(arrayOrHashmap, rowNb) {
   if (arrayOrHashmap instanceof Array) {
      return getPartialArray(arrayOrHashmap, rowNb);
   }
   if (typeof arrayOrHashmap === 'object') {
      return getPartialHashmap(arrayOrHashmap, rowNb);
   }
}
function getPartialArray(array, rowNb) {
   if (typeof rowNb === 'undefined') {
      rowNb = 10;
   }
   var partialArray = [];
   var isPartial = true;
   if (array.length < rowNb) {
      rowNb = array.length;
      isPartial = false;
   }
   var partialArrayLength = Math.min(array.length, rowNb);
   for (var pInd = 0; pInd < partialArrayLength; pInd++) {
      partialArray.push(array[pInd]);
   }
   return { isPartial : isPartial, res : partialArray };
}
function getPartialHashmap(hashmap, rowNb) {
   if (typeof rowNb === 'undefined') {
      rowNb = 10;
   }
   var partialHashmap = {};
   var partialNb = 0;
   for (var key in hashmap) {
      if (partialNb++ === rowNb) {
         return { isPartial : true, res : partialHashmap };
      }
      partialHashmap[key] = hashmap[key];
   }
   return { isPartial : false, res : partialHashmap };
}


function setUrl(relativeUrl, dontUpdateDisplay) {
   if (dontUpdateDisplay || !doUpdateDisplay) {
      return false;
   }
   if (!relativeUrl || relativeUrl === "") {
      $('.myurl').val('');
      document.getElementById('mydata').innerHTML = '';
   } else {
      if (typeof relativeUrl !== 'object') {
         relativeUrl = parseUri(relativeUrl);
      }
      // build unencoded URI, for better readability :
      var unencodedRelativeUrl = '/dc/' + (relativeUrl.version != null && typeof relativeUrl.version !== 'undefined' // NOT (version) which is false
         ? 'h' : 'type') + '/' + relativeUrl.modelType;
      if (relativeUrl.id) {
         unencodedRelativeUrl += '/' + relativeUrl.id;
      }
      if (relativeUrl.version != null && typeof relativeUrl.version !== 'undefined') { // NOT (version) which is false
          unencodedRelativeUrl += '/' + relativeUrl.version;
       }
      if (relativeUrl.query) {
         unencodedRelativeUrl += '?' + buildUriQuery(parseUriQuery(relativeUrl.query), true);
      }
      $('.myurl').val(unencodedRelativeUrl);
      $('#mydata').html('...');
   }
   return false;
}
// the opposite of setUrl()
function encodeUri(unencodedUri) {
   var uri = parseUri(unencodedUri);
   if (uri.query) {
      uri.query = buildUriQuery(parseUriQuery(uri.query, true));
   }
   return uri;
}
function setError(errorMsg) {
   if (doUpdateDisplay) {
      $('#mydata').text(errorMsg);
   }
   return false;
}
function requestToRelativeUrl(request) {
   var relativeUrl = request.path;
   if (request._query) {
      relativeUrl += '?' + request._query;
   }
   return relativeUrl;
}


function getProject() {
   if (!window.currentProject || window.currentProject.length === 0) {
      return 'oasis.sandbox'; // by default don't pollute anything // oasis.sandbox oasis.main (oasis.sample)
   }
   return window.currentProject;
}
function setProject(newProject) {
   window.currentProject = newProject;
   
   initProjectPortal();
}
function initProjectPortal(options) {
   findData(buildProjectPortalQuery(options), projectPortalSuccess,
         null, null, 25, {'X-Datacore-View':' '}, options);
}
function buildProjectPortalQuery(options) {
   var query = 'dcmo:model_0?';
   if (!options || !options.pureMixins || options.storageModels) {
      query += 'dcmo:isStorage=true';
   } else {
      query += 'dcmo:isInstanciable=!=true';
   }
   if (!options || !options.global || options.local) {
      query += '&dcmo:pointOfViewAbsoluteName=' + getProject();
   }
   return query;
}
function buildProjectPortalQueryLink(options, linkText) {
   return '<a href="/dc/type/' + buildProjectPortalQuery(options) + '" class="dclink" onclick="'
      + 'javascript:return findData($(this).attr(\'href\'), projectPortalSuccess, null, null, 25, {\'X-Datacore-View\':\' \'}, '
      + (options ? stringifyForAttribute(options) : 'null') + ');">' + linkText + '</a>';
}
function buildProjectPortalTitleHtml(options) {
   if (!options) {
      options = { pureMixins:false, global:false }; // to ease up building alt options 
   }
   var html = '';
   var projectName = getProject();
   html += '<a href="/dc/type/dcmp:Project_0/' + projectName + '" class="dclink" onclick="'
   + 'javascript:return getData($(this).attr(\'href\'));'
   + '">' + projectName + '</a>\'s '
   if (!options.pureMixins || options.storageModels) {
      html += 'storage models &amp; their stored models (or '
         + buildProjectPortalQueryLink({ pureMixins:true, global:options.global }, 'pure mixins') + '), ';
   } else {
      html += 'pure mixins &amp; their uses (or '
         + buildProjectPortalQueryLink({ global:options.global }, 'storage models') + '), ';
   }
   if (!options.global || options.local) {
      html += 'local to ' + getProject() + ' (or '
         + buildProjectPortalQueryLink({ pureMixins:options.pureMixins, global:true }, 'all') + ')';
   } else {
      html += 'global (or '
         + buildProjectPortalQueryLink({ pureMixins:options.pureMixins }, 'local to ' + getProject()) + ')';
   }
   html += ' :<br/>';
   return html;
}
function projectPortalSuccess(storageModels, relativeUrl, data, options) {
   setUrl(relativeUrl); // because not set in findData because it uses this custom handler
   setError('');
   var html = buildListOfStorageModelLinks(storageModels);
   html = addPaginationLinks(data.request, html, storageModels,
           'function (storageModels, relativeUrl, data) { projectPortalSuccess(storageModels, relativeUrl, data, '
           + stringifyForAttribute(options) + '); }');
   html = buildProjectPortalTitleHtml(options) + html;
   $('.mydata').html(html);
}
function buildListOfStorageModelLinks(models) {
   var html = '';
   for (var mInd in models) {
      var modelName = parseUri(models[mInd]['@id']).id;
      // (or 'dcmo:name' but not returned in this view)
      html += '- <a href="/dc/type/dcmo:model_0/' + modelName + '" class="dclink" onclick="'
         + 'javascript:return getData($(this).attr(\'href\'));'
         + '">' + modelName + '</a> : its stored '
         + '<a href="/dc/type/dcmo:model_0?dcmo:storageModel=' + modelName + '" class="dclink" onclick="'
         + 'javascript:return findData($(this).attr(\'href\'), null, null, null, 25, {\'X-Datacore-View\':\' \'});'
         + '">models</a> and '
         + '<a href="/dc/type/' + modelName + '" class="dclink" onclick="'
         + 'javascript:return findData($(this).attr(\'href\'), null, null, null, 25, {\'X-Datacore-View\':\' \'});'
         + '">all their resources</a>...'
         + '<br/>';
   }
   return html;
}


///////////////////////////////////////////////////
// API

///////////////////////////////////////////////////
// READ

// relativeUrl must be an encoded URI (encode it using builUri and buildUriQuery)
// or a {modelType, query} object where query is encoded (encode it
// using buildUriQuery)
// success : success handler, providing it disables displaying results in playground
// optional : success, error, start (else 0, max 500), limit (else 10 !!! max 100 !)
// optionalHeaders : 'Accept':'text/x-nquads' for RDF, 'X-Datacore-View', 'X-Datacore-Project'...
// handlerOptions : business options to pass to (outside call conf i.e. path / query / headers
// which will be in data._request anyway)
function findDataByType(relativeUrl, success, error, start, limit, optionalHeaders, handlerOptions) {
   if (typeof relativeUrl === 'string') {
      // NB. modelType encoded as URIs should be, BUT must be decoded before used as GET URI
      // because swagger.js re-encodes
      relativeUrl = parseUri(relativeUrl);
   }
   setUrl(relativeUrl, success);
   var swaggerParams = {type:relativeUrl.modelType, '#queryParameters':relativeUrl.query,
         Authorization:getAuthHeader()};
   if (start) {
      swaggerParams.start = start;
   }
   if (limit) {
      swaggerParams.limit = limit;
   }
   var supplParams = null; // handlerOptions == null ? null : {parent:handlerOptions}; // NO else no error callback
   if (optionalHeaders) {
      for (var headerName in optionalHeaders) {
         swaggerParams[headerName] = optionalHeaders[headerName];
      }
      if (optionalHeaders['Accept']) {
         supplParams = supplParams == null ? {} : supplParams;
         supplParams['responseContentType'] = optionalHeaders['Accept']; // for RDF (no other way)
      }
   }
   if (!swaggerParams['X-Datacore-Project']) {
      swaggerParams['X-Datacore-Project'] = getProject();
   }
   var mySuccess = function(data) { // , handlerOptions
      var resResourcesOrText;
      if (data.request._headers['X-Datacore-Debug'] === 'true') {
         // displaying debug info (index used) : (NB. not in other operations)
         var explainRes = displayJsonObjectResult(data, success);
         if (explainRes) {
            resResourcesOrText = explainRes.results;
         }
      } else {
         var contentType = data.request._headers['Accept'];
         if (contentType && contentType.indexOf('text/') === 0) { // ex. RDF : 'text/x-nquads'
            resResourcesOrText = displayTextResult(data, success);
         } else {
            resResourcesOrText = displayJsonListResult(data, success);
         }
      }
      if (success) {
         success(resResourcesOrText, requestToRelativeUrl(data.request), data, handlerOptions);
      } else {
         setError('');
      }
   };
   var myError = function(data) { // , handlerOptions
      setError(data._body._body);
      if (error) {
         error(data, requestToRelativeUrl(data.request), handlerOptions);
      }
   };
   if (supplParams) {
      dcApi.dc.findDataInType(swaggerParams, supplParams, mySuccess);
      // NB. to still allow RDF, see https://github.com/swagger-api/swagger-js/issues/101
      // "Unable to pass 'opts' for method invocation (swagger.js)?"
   } else {
      dcApi.dc.findDataInType(swaggerParams, mySuccess, myError);
   }
   return false;
}
// relativeUrl must be an encoded URI (encode it using builUri)
// or a {modelType, id} object
// optional : success, error
function getData(relativeUrl, success, error, optionalHeaders, handlerOptions) {
   if (typeof relativeUrl === 'string') {
      // NB. modelType encoded as URIs should be, BUT must be decoded before used as GET URI
      // because swagger.js re-encodes (for resourceId, per path element because __unencoded__-prefixed per hack)
      relativeUrl = parseUri(relativeUrl);
   }
   setUrl(relativeUrl, success);
   var swaggerParams = {type:relativeUrl.modelType, __unencoded__iri:relativeUrl.id,
           'If-None-Match':-1, Authorization:getAuthHeader()};
   var supplParams = null; // handlerOptions == null ? null : {parent:handlerOptions}; // NO else no error callback
   if (optionalHeaders) {
      for (var headerName in optionalHeaders) {
         swaggerParams[headerName] = optionalHeaders[headerName];
      }
      if (optionalHeaders['Accept']) {
         supplParams = supplParams == null ? {} : supplParams;
         supplParams['responseContentType'] = optionalHeaders['Accept']; // for RDF (no other way)
      }
   }
   if (!swaggerParams['X-Datacore-Project']) {
      swaggerParams['X-Datacore-Project'] = getProject();
   }
   var mySuccess = function(data) {
      var resResourceOrText;
      var contentType = data.request._headers['Accept'];
      if (contentType && contentType.indexOf('text/') === 0) { // ex. RDF : 'text/x-nquads'
         resResourceOrText = displayTextResult(data, success);
      } else {
         resResourceOrText = displayJsonObjectResult(data, success);
      }
      if (success) {
         success(resResourceOrText, relativeUrl, data, handlerOptions);
      } else {
         setError('');
      }
   };
   var myError = function(data) {
      setError(data._body._body);
      if (error) {
         error(data, relativeUrl, handlerOptions);
      }
   };
   var dcApiFunction = dcApi.dc.getData;
   if (relativeUrl.version != null && typeof relativeUrl.version !== 'undefined') { // NOT (version) which is false
      dcApiFunction = dcApi.dc.findHistorizedResource;
      swaggerParams['version'] = relativeUrl.version + ''; // toString else swagger hack encodeUriPathComponent fails
   }
   if (supplParams) {
      dcApiFunction(swaggerParams, supplParams, mySuccess);
      // NB. to still allow RDF, see https://github.com/swagger-api/swagger-js/issues/101
      // "Unable to pass 'opts' for method invocation (swagger.js)?"
   } else {
      dcApiFunction(swaggerParams, mySuccess, myError);
   }
   return false;
}
// relativeUrl must be an encoded URI (encode it using builUri and buildUriQuery)
// or a {containerUrl (optional), modelType, id, query} object where query is encoded (encode it
// using buildUriQuery)
// optional : success, error
function findData(relativeUrl, success, error, start, limit, optionalHeaders, handlerOptions) {
   if (typeof relativeUrl === 'string') {
      relativeUrl = parseUri(relativeUrl);
   }
   if (relativeUrl.query !== null || relativeUrl.id === null) { // a query or no id
      return findDataByType(relativeUrl, success, error, start, limit, optionalHeaders, handlerOptions);
   }
   return getData(relativeUrl, success, error, optionalHeaders, handlerOptions);
}
function getPreviousData(relativeUrl, success, error, optionalHeaders, handlerOptions) {
   var historyUrl;
   if (typeof relativeUrl === 'string') {
      // NB. modelType encoded as URIs should be, BUT must be decoded before used as GET URI
      // because swagger.js re-encodes
       relativeUrl = parseUri(relativeUrl);
   }
   if (relativeUrl.version != null && typeof relativeUrl.version !== 'undefined') { // NOT (version) which is false
      // get previous one :
      var previousUrl = relativeUrl.uri.replace(new RegExp('/'
            + relativeUrl.version + '$'), '/' + (relativeUrl.version - 1));
      getData(previousUrl, success, error, optionalHeaders, handlerOptions);
      return;
   }
   // first retrieve last version :
   getData(relativeUrl, function(resResourceOrText, relativeUrl, data) { // , handlerOptions
      // get previous one :
      var version = parseInt(resResourceOrText['o:version']);
      var historyUrl = relativeUrl.uri.replace('/type', '/h') + '/' + (version - 1);
      getData(historyUrl, success, error, optionalHeaders, handlerOptions);
   }, null, {'X-Datacore-View':' ',
      'X-Datacore-Project':(optionalHeaders ? optionalHeaders['X-Datacore-Project'] : null)});
}


///////////////////////////////////////////////////
// WRITE

// relativeUrl must be an encoded URI (encode it using builUri) or a {modelType} object,
// resources can be a single resource or an array
// optional : success, error
function postAllDataInType(relativeUrl, resources, success, error, optionalHeaders, handlerOptions) {
   if (typeof relativeUrl === 'string') {
      // NB. modelType encoded as URIs should be, BUT must be decoded before used as GET URI
      // because swagger.js re-encodes
      relativeUrl = parseUri(relativeUrl);
   }
   setUrl(relativeUrl, success);
   if (!(resources instanceof Array)) { // single resource
      resources = [ resources ];
   }
   var swaggerParams = {type:relativeUrl.modelType, body:JSON.stringify(resources, null, null),
         Authorization:getAuthHeader()};
   if (optionalHeaders) {
      for (var headerName in optionalHeaders) {
         swaggerParams[headerName] = optionalHeaders[headerName];
      }
   }
   if (!swaggerParams['X-Datacore-Project']) {
      swaggerParams['X-Datacore-Project'] = getProject();
   }
   dcApi.dc.postAllDataInType(swaggerParams,
      function(data) {
         var resResources = displayJsonListResult(data, success);
         if (success) {
    	    success(resResources, resources, data, handlerOptions);
         } else {
            setError('');
         }
      },
      function(data) {
         setError(data._body._body);
         if (error) {
            error(data, resources, handlerOptions);
         }
         /*if (error._body._body.indexOf("already existing") != -1) { // TODO better
          findDataByType(relativeUrl, callback);
         }*/
      });
   return false;
}
// NB. no postDataInType in swagger (js) API (because would be a JAXRS conflict)
// postDataInType would only differ on dcApi.dc.postDataInType function, TODO better & also for put
// TODO putDataInType and use it...

// resource's o:version must be up to date
// optional : success, error
function deleteDataInType(resource, success, error, optionalHeaders, handlerOptions) {
   // NB. modelType encoded as URIs should be, BUT must be decoded before used as GET URI
   // because swagger.js re-encodes (for resourceId, per path element because __unencoded__-prefixed per hack)
   var parsedUri = parseUri(resource["@id"]);
   setUrl(parsedUri, success);
   var swaggerParams = {type:parsedUri.modelType, __unencoded__iri:parsedUri.id,
         'If-Match':resource["o:version"], Authorization:getAuthHeader()};
   if (optionalHeaders) {
      for (var headerName in optionalHeaders) {
         swaggerParams[headerName] = optionalHeaders[headerName];
      }
   }
   if (!swaggerParams['X-Datacore-Project']) {
      swaggerParams['X-Datacore-Project'] = getProject();
   }
   dcApi.dc.deleteData(swaggerParams,
      function(data) {
         var resResource = displayJsonObjectResult(data, success);
         if (success) {
            success(resResource, resource, data, handlerOptions);
         }
      },
      function(data) {
         setError(data._body._body);
         if (error) {
            error(data, resource, handlerOptions);
         }
      });
   return false;
}

// resources can be a single resource or an array
// optional : success, error
function postAllData(resources, success, error, optionalHeaders, handlerOptions) {
   if (!(resources instanceof Array)) { // single resource
      resources = [ resources ];
   }
   var swaggerParams = {body:JSON.stringify(resources, null, null),
         Authorization:getAuthHeader()};
   if (optionalHeaders) {
      for (var headerName in optionalHeaders) {
         swaggerParams[headerName] = optionalHeaders[headerName];
      }
   }
   if (!swaggerParams['X-Datacore-Project']) {
      swaggerParams['X-Datacore-Project'] = getProject();
   }
   dcApi.dc.postAllData(swaggerParams,
      function(data) {
         var resResources = displayJsonListResult(data, success);
         if (success) {
            success(resResources, resources, data, handlerOptions);
         }
      }, function(data) {
         setError(data._body._body);
         /*if (data._body._body.indexOf("already existing") != -1) { // TODO better
            findDataByType(relativeUrl, callback);
         }*/
         if (error) {
            error(data, resources, handlerOptions); // , resources // NB. always undefined ! NOO ?!
         }
      });
    return false;
}

// displays as editable data or post current editable data, depending on whether myeditabledata displayed or not
function editOrPostData(relativeUrl) {
   if ($('#myeditabledata').css('display') !== 'block') {
      findData(relativeUrl, displayResourcesAsEditable);
   } else { // 'none'
      var editedResources = eval('[' + $('#myeditabledata').val() + ']')[0];
      var typeOfEditedResources = typeof editedResources;
      if (typeOfEditedResources === 'object') { // (also includes Array)
         postAllData(editedResources, function() {
            switchToEditable(false);
            findData(relativeUrl);
         });
      }
   }
}

function deleteResources(relativeUrl) {
   findData(relativeUrl, deleteResourcesCallback);
}
function deleteResourcesCallback(resources, relativeUrl) {
   if (!resources) {
      return;
   }
   var resources = resources instanceof Array ? resources : [ resources ];
   if (resources.length == 0) {
      return;
   }
   if (!window.confirm('You\'re about to delete ' + resources.length
            + ' resources coming from query ' + relativeUrl.uri + ' ! are you sure ?')) {
      return;
   }
   deleteNextResourcesCallback(null, null, null,
         { resources : resources, i : 0, finalCallback : displayDeletedResources,
         optionalHeaders : null, uriToSuccessStatus : {}, uriToErrorInfo : {} });
}
// deleteState = { resources : resources, i : -1 }
function deleteNextResourcesCallback(resResource, resource, data, deleteState) {
   if (!deleteState) { // error case
      deleteState = data;
      data = resResource;
   }
   if (data && data._raw) {
      if (~~(data._raw.statusCode / 100) !== 2) {
         var errInfo = { statusCode : data._raw.statusCode };
         var resBody = data._raw.xhr.response;
         if (resBody && resBody.length !== 0) {
            errInfo['body'] = data._raw.xhr.response;
         }
         deleteState.uriToErrorInfo[resource['@id']] = errInfo;
      } else {
         deleteState.uriToSuccessStatus[resource['@id']] = data._raw.statusCode;
      }
   } // else first time
   if (deleteState.resources.length === deleteState.i) {
      displayDeletedResources(deleteState);
      return;
   }
   var i = deleteState.i++;
   deleteDataInType(deleteState.resources[i],
         deleteNextResourcesCallback, deleteNextResourcesCallback,
         deleteState.optionalHeaders, deleteState);
}
function displayDeletedResources(deleteState) {
   var html;
   if (deleteState.uriToErrorInfo.length === 0) {
      html = 'Deleted all ' + Object.keys(deleteState.uriToSuccessStatus).length + ' resources.';
   } else {
      html = 'Deleted ' + Object.keys(deleteState.uriToSuccessStatus).length + ' resources over '
            + deleteState.resources.length + ', detailed errors :';
      html += '<br/>';
      for (var uri in deleteState.uriToErrorInfo) {
         html += toolifyDcResourceUri(uri) + ' : ' + JSON.stringify(
               deleteState.uriToErrorInfo[uri], null, '\t').replace(/\n/g, '<br/>');
         html += '<br/>';
      }
   }
   $('.mydata').html(html);
}
// still TODO :
function deleteResourcesOrModel(resource, success, error, optionalHeaders, handlerOptions) {
   var isModel = resources[0]['dcmo:name'] ? true : false;  
}
function deleteModelAndItsResources(resource, success, error, optionalHeaders, handlerOptions) {
    
}


///////////////////////////////////////////////////
// DISPLAY CALLBACKS

var doUpdateDisplay = true;

// can be used as findData() success callback right away
function displayResourcesAsEditable(resources) {
   switchToEditable(true);
   $('#myeditabledata').val(JSON.stringify(resources, null, 3));
}
function switchToEditable(doSwitch) {
   $('#mynoteditabledata').css('display', doSwitch ? 'none' : 'block');
   $('#myeditabledata').css('display', !doSwitch ? 'none' : 'block');
   $('#editOrPost').html(doSwitch ? 'POST' : 'edit');
}

function displayTextResult(data, dontUpdateDisplay, displayAsEditable) {
   if (!dontUpdateDisplay && doUpdateDisplay) {
      var prettyText = data.content.data;
      $('.mydata').text(prettyText);
      if (data.request._query !== null && data.request._query.trim().length !== 0
            || parseUri(data.request.path).id === null) { // a query or no id
         var prettyHtml = $('.mydata').html();
         prettyHtml = addPaginationLinks(data.request, prettyHtml, null);
         $('.mydata').html(prettyHtml);
      }
      switchToEditable(false);
   }
   return data.content.data;
}

function displayJsonObjectResult(data, dontUpdateDisplay) {
   var resource = eval('[' + data.content.data + ']')[0];
   if (!dontUpdateDisplay && doUpdateDisplay) {
      var prettyJson = toolifyDcResource(resource, 0); // ,  parseUri(data.request.path).modelType // , upperResource, keyPathInResource
      //var prettyJson = JSON.stringify(resource, null, '\t').replace(/\n/g, '<br/>');
      //prettyJson = toolifyDcResourceJson(prettyJson);
      $('.mydata').html(prettyJson);
      switchToEditable(false);
   }
   return resource;
}

function displayJsonListResult(data, dontUpdateDisplay) {
   var resResources = eval(data.content.data);
   if (!dontUpdateDisplay && doUpdateDisplay) {
      var prettyJson = toolifyDcList(resResources, 0, null, parseUri(data.request.path).modelType);
      prettyJson = addPaginationLinks(data.request, prettyJson, resResources);
      ///var prettyJson = JSON.stringify(resResources, null, '\t').replace(/\n/g, '<br>');
      ///prettyJson = toolifyDcResourceJson(prettyJson);
      $('.mydata').html(prettyJson);
      switchToEditable(false);
   }
   return resResources;
}

// params : request with path, _query and _headers ;
// prettyJson which is enriched with links ;
// resList whose size is used as paginating condition.
function addPaginationLinks(request, prettyJson, resList, successFunctionName) {
   // adding "..." link for pagination :
   // NB. everything is already encoded
   if (!successFunctionName) {
       successFunctionName = 'null';
   }
   var start = 0;
   var limit = dcConf.queryDefaultLimit; // from conf
   var query = '';
   if (request._query) {
      // at least must get actual limit to know whether there might be more data
      // but it's easier to also get start and build query at the same time
      var splitQuery = request._query.split('&');
      for (var critInd in splitQuery) {
         var criteria = splitQuery[critInd];
         if (criteria.indexOf('start=') !== -1) {
            start = parseInt(criteria.substring(criteria.indexOf('=') + 1), 10);
         } else if (criteria.indexOf('limit=') !== -1) {
            limit = parseInt(criteria.substring(criteria.indexOf('=') + 1), 10);
         } else {
            query += '&' + criteria;
         }
      }
   }

   if (start !== 0) {
      var previousStart = Math.max(0, start - limit);
      var relativeUrl = request.path + '?start=' + previousStart + '&limit=' + limit + query;
      var headers = {};
      for (var hInd in request._headers) {
         if (hInd === 'Accept' || hInd.indexOf('X-Datacore-') === 0) {
            headers[hInd] = request._headers[hInd]; // Accept for RDF, else 'X-Datacore-View'...
         }
      }
      prettyJson = '<a href="' + relativeUrl + '" class="dclink" onclick="'
            + 'javascript:return findDataByType($(this).attr(\'href\'), ' + successFunctionName + ', null, null, null, '
            + stringifyForAttribute(headers) + ');'
            + '">...</a>' + lineBreak(0) + prettyJson;
   }
   if (!resList || typeof resList === 'string' // RDF case : always display
         || resList.length === limit) {
      var nextStart = start + limit;
      var relativeUrl = request.path + '?start=' + nextStart + '&limit=' + limit + query;
      var headers = {};
      for (var hInd in request._headers) {
          if (hInd === 'Accept' || hInd.indexOf('X-Datacore-') === 0) {
             headers[hInd] = request._headers[hInd]; // Accept for RDF, else 'X-Datacore-View'...
          }
       }
      prettyJson += lineBreak(0) + '<a href="' + relativeUrl + '" class="dclink" onclick="'
            + 'javascript:return findDataByType($(this).attr(\'href\'), ' + successFunctionName + ', null, null, null, '
            + stringifyForAttribute(headers) + ');'
            + '">...</a>';
   }
   return prettyJson;
}


///////////////////////////////////////////////////
// PLAYGROUND ADVANCED BROWSING

function findLinkedData(resourceUri, linkingModelType, queryFieldPath) {
   var parsedUri = parseUri(resourceUri);
   if (typeof linkingModelType === 'undefined') {
      linkingModelType = parsedUri.modelType;
   }
   if (typeof queryFieldPath === 'undefined') {
      queryFieldPath = '';
   }
   ///var depth = queryFieldPath.split('.').length - 2; // 0 for dcmo:globalFields.dcmf:resourceType
   var isModel = parsedUri.modelType.indexOf('dcmo:model_') === 0;
   if (isModel) { // lookup instances in this model :
      findDataByType({ modelType : parsedUri.id }); // NB. works even if not storage thanks to polymorphism
   } else { // lookup models linking to this one and display them and their resources :
      var linkedModelUrl = { modelType : 'dcmo:model_0', query : new UriQuery(
         'dcmo:globalFields.' + queryFieldPath + 'dcmf:resourceType', linkingModelType).s() };
      setUrl(linkedModelUrl);
      findDataByType(linkedModelUrl, function(topLevelLinkingModelResources) {
         // also list fields using embedded subresource dcmf:field_0 stored in dcmo:mixin_0 :
         findDataByType({ modelType : 'dcmo:model_0', query : new UriQuery(
            'dcmo:globalFields.' + queryFieldPath + 'dcmf:listElementField.dcmf:resourceType', linkingModelType).s() },
               function(topLevelListLinkingModelResources) {
            getData(parsedUri, function(linkedResource) {
               displayModelAndResourceLinks(linkedResource, parsedUri, queryFieldPath, linkingModelType,
                     topLevelLinkingModelResources, topLevelListLinkingModelResources);
            });
         },null,null,100);
         // TODO LATER anyway, it doesn't handle all list + field combinations ex. list.field.field,
         // so thinkg of more queriable storage for models ex. to allow to query fields at every depth :
         // ex. (additional) per field collection entity with materialized path in mongo, or relational database, or (db'd, collaborative) EMF, or triplestore !!
         
         // NB. simpler version without lists :
         /*getData(parsedUri, function(linkedResource) {
            displayModelAndResourceLinks(linkedResource, parsedUri, linkingModelType,
                  topLevelLinkingModelResources, [], depth);
         });*/
      });
      // NB. for subresources : models having this one as mixin can be found by ":" on @type
      // TODO LATER also on dcmo:globalFields.dcmf:mapFields.dcmf:resourceType for top level maps' fields...
   }
   return false;
}
// displays linking models and their resources
function displayModelAndResourceLinks(linkedResource, parsedUri, queryFieldPath, linkingModelType,
      linkingModels, listLinkingModels) {
   var html = '';
   html += displayModelAndResourceFieldLinks(linkingModels, parsedUri, queryFieldPath, linkingModelType, 'field');
   html += lineBreak(0);
   html += displayModelAndResourceFieldLinks(listLinkingModels, parsedUri, queryFieldPath + 'dcmf:listElementField.', linkingModelType, 'list field');
   html += displayModelMixinLinks(linkedResource, parsedUri, linkingModelType);
   $('.mydata').html(html);
}
function displayModelAndResourceFieldLinks(linkingModels, parsedUri, queryFieldPath, linkingModelType, fieldKind) {
   var html;
   var depth = queryFieldPath.split('.').length - 1; // 1 for (dcmo:globalFields).dcmo:globalFields(.dcmf:resourceType)
   if (linkingModels.length === 0) {
      html = 'No model links through '
         + '<a href="' + buildRelativeUrl('dcmo:model_0', linkingModelType) + '" class="dclink" onclick="'
         + 'javascript:return getData($(this).attr(\'href\'));'
         + '">' + linkingModelType + '</a>'
         + ' by a ' + depth + '-deep ' + fieldKind + ' to ' + lineBreak(1) + toolifyDcResourceUri(parsedUri.uri) + ' .';
   } else {
      html = 'There are ' + linkingModels.length + ' model(s) linking through '
         + '<a href="' + buildRelativeUrl('dcmo:model_0', linkingModelType) + '" class="dclink" onclick="'
         + 'javascript:return getData($(this).attr(\'href\'));'
         + '">' + linkingModelType + '</a>'
         + ' by a ' + depth + '-deep ' + fieldKind + ' to ' + lineBreak(1) + toolifyDcResourceUri(parsedUri.uri) + ' :'; // NB. line break is below
      for (var mInd in linkingModels) {
         var linkingModel = linkingModels[mInd];
         var criteria = {};
         criteria[queryFieldPath + 'dcmf:resourceType'] = linkingModelType;
         var field = findResource(linkingModel['dcmo:globalFields'], criteria);
         var modelType = linkingModel['dcmo:name'];
         html += lineBreak(0) + '- ';
         html += '<a href="' + buildRelativeUrl('dcmo:model_0', modelType) + '" class="dclink" onclick="'
            + 'javascript:return getData($(this).attr(\'href\'));'
            + '">' + modelType + '</a>'
         html += ' - resources : ';
         //html += toolifyDcResourceValue(model, null, 1);
         html += dcConf.containerUrl + '/dc/'
            + '<a href="' + buildRelativeUrl('dcmo:model_0', modelType) + '" class="dclink" onclick="'
            + 'javascript:return getData($(this).attr(\'href\'));'
            + '">type</a>'
            + '/'
            + '<a href="' + buildRelativeUrl(modelType) + '?' + new UriQuery(field['dcmf:name'], parsedUri.uri).s() + '" class="dclink" onclick="'
            + 'javascript:return findDataByType($(this).attr(\'href\'));'
            + '">' + modelType + '</a>'
            + '...';
      }
   }
   queryFieldPath = 'dcmo:globalFields.' + queryFieldPath;
   html += lineBreak(0) + 'You can also try '
   + '<a href="' + buildRelativeUrl('dcmo:model_0') + '?' + new UriQuery(
      'builddcmo:globalFields.' + queryFieldPath + 'dcmf:resourceType', linkingModelType).s() + '" class="dclink" onclick="'
   + 'javascript:return findLinkedData(\'' + buildUri(parsedUri.modelType, parsedUri.id) + '\', \''
   + linkingModelType + '\', \'' + queryFieldPath + '\');'
   + '">at further depth</a>'
   + "...";
   return html;
}
function displayModelMixinLinks(linkedResource, parsedUri, linkingModelType) {
   var html = '';
   var mixins = linkedResource['@type'];
   if (mixins.length > 1) {
      html += lineBreak(0) + 'You can also try its other mixins :';
      for (var mInd in mixins) {
         var mixinName = mixins[mInd];
         if (mixinName === linkingModelType) {
            continue;
         }
         html += lineBreak(1) + '- ';
         html +=  '<a href="' + buildRelativeUrl('dcmo:model_0') + '?' + new UriQuery(
               'dcmo:globalFields.dcmf:resourceType', mixinName).s() + '" class="dclink" onclick="'
            + 'javascript:return findLinkedData(\'' +  buildUri(parsedUri.modelType, parsedUri.id) + '\', \'' + mixinName + '\');'
            + '">' + mixinName + '</a>'
            + "...";
      }
   }
   return html;
}


///////////////////////////////////////////////////
// MODEL MANIPULATION

function findResource(resources, criteria) {
   for (var rInd in resources) {
      var resource = resources[rInd];
      var found = true;
      for (var key in criteria) {
         var keyPathElts = key.split('.');
         if (!checkResourceCriteria(resource, keyPathElts, 0, criteria[key])) {
            found = false;
            break;
         }
      }
      if (found) {
         return resource;  
      }
   }
   return null;
}

function checkResourceCriteria(subresourceOrValue, keyPathElts, depth, value) {
   if (subresourceOrValue instanceof Array) {
      for (var i in subresourceOrValue) {
         if (checkResourceCriteria(subresourceOrValue[i], keyPathElts, depth, value)) { // same depth
            return true;
         }
      }
      return false;
   } else if (depth == keyPathElts.length) {
      return subresourceOrValue == value;
   } else {
      subresourceOrValue = subresourceOrValue[keyPathElts[depth]];
      if (typeof subresourceOrValue === 'undefined') { // ex. no dcmf:listElementField.dcmf:resourceType in a string field
          return false;
      }
      return checkResourceCriteria(subresourceOrValue, keyPathElts, depth + 1, value);
   }
}