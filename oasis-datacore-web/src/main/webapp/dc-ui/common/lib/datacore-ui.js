//var containerUrl = "http://data.oasis-eu.org/"; // rather in dcConf filled at init by /dc/playground/configuration


   
   function encodeUriPath(uriPath) {
      if (uriPath.indexOf("/") === -1) {
         return encodeUriPathComponent(uriPath);
      }
      var slashSplitPath = uriPath.split('/');
      for (var pathCptInd in slashSplitPath) {
         slashSplitPath[pathCptInd] = encodeUriPathComponent(slashSplitPath[pathCptInd]); // encoding ! NOT encodeURIComponent
      }
      return slashSplitPath.join('/');
      //return encodeURI(idValue); // encoding !
      // (rather than encodeURIComponent which would not accept unencoded slashes)
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
   function buildUri(typeName, id, shouldEncodeId) {
      // encoding ! NOT encodeURIComponent
      return dcConf.containerUrl + "/dc/type/" + encodeUriPathComponent(typeName)
            + "/" + (shouldEncodeId ? encodeIdSaveIfNot(id) : id);
   }
   // also supports relative & no id after modelType
   function getModelTypeFromUri(subResourceUri) { // TODO regex
      var mInd = subResourceUri.indexOf('dc/type/') + 'dc/type/'.length;
      var nextSlashInd = subResourceUri.indexOf('/', mInd);
      return decodeURIComponent(subResourceUri.substring(mInd,
            (nextSlashInd !== -1) ? nextSlashInd : subResourceUri.length));
   }
   function getIdFromUri(subResourceUri) { // TODO regex
      return decodeURI(subResourceUri.substring(subResourceUri
            .indexOf('/', subResourceUri.indexOf('/dc/type/') + '/dc/type/'.length) + 1));
   }
   // only supports relative uri (iri) ex. /dc/type/model/id and query, but modelType at least is required
   //var dcResourceIriRegex = /^\/+dc\/+type\/+([^\/\?]+)\/*([^\?]+)?\??(.*)$/g; // NOO seems stateful, else sometimes matches gets null
   function parseIri(resourceIri) { // TODO regex
      /*var encModelType = resourceIri.substring(0, resourceIri.indexOf("/"));
      var encId = decodeURI(resourceIri.substring(resourceIri.indexOf("/") + 1));
      return {
         containerUrl : dcConf.containerUrl,
         modelType : decodeURIComponent(encModelType),
         id : decodeURI(encId),
         uri : dcConf.containerUrl + resourceIri
         };*/
      var matches = /^\/+dc\/+type\/+([^\/\?]+)\/*([^\?]+)?\??(.*)$/g.exec(resourceIri);
      var modelType = decodeURIComponent(matches[1]); // required
      // NB. modelType encoded as URIs should be, BUT must be decoded before used as GET URI
      // because swagger.js re-encodes
      var id = matches[2];
      var query = matches[3]; // no decoding, else would need to be first split along & and =
      if (id) {
         id = decodeURI(id);
      } else {
         id = null;
      }
      if (!query) {
         query = null;
      }
      return {
         containerUrl : dcConf.containerUrl,
         modelType : modelType,
         id : id,
         query : query,
         uri : dcConf.containerUrl + resourceIri, // NOT encoded !!
         iri : resourceIri // NOT encoded !!
         };
   }
   //var dcResourceUriRegex = /^http:\/\/data\.oasis-eu\.org\/dc\/type\/([^\/]+)\/(.+)$/g;
   //var dcResourceUriRegex = /^(http[s]?):\/\/+([^\/]+)\/+dc\/+type\/+([^\/\?]+)\/*([^\?]+)?\??(.*)$/g; // NOO seems stateful, else sometimes matches gets null
   // also supports relative uri (iri) and query, but modelType is required
   function parseUri(resourceUri) { // TODO regex
      if (resourceUri.indexOf('http') !== 0) {
         return parseIri(resourceUri);
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
      var matches = /^(http[s]?):\/\/+([^\/]+)\/+dc\/+type\/+([^\/\?]+)\/*([^\?]+)?\??(.*)$/g.exec(resourceUri);
      var containerUrl = matches[1] + '://' + matches[2];
      var modelType = decodeURIComponent(matches[3]); // required
      // NB. modelType encoded as URIs should be, BUT must be decoded before used as GET URI
      // because swagger.js re-encodes
      var id = matches[4];
      var query = matches[5]; // no decoding, else would need to be first split along & and =
      if (id) {
         id = decodeURI(id);
      } else {
         id = null;
      }
      if (!query) {
         query = null;
      }
      return {
         //containerUrl : dcConf.containerUrl,
         containerUrl : containerUrl,
         modelType : modelType,
         id : id,
         query : query,
         uri : resourceUri, // NOT encoded !!
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
   function getModelTypeUrl(uri) {
      if (typeof uri === 'object') {
         uri = uri.uri;
      }
      var pInd = uri.indexOf("/dc/type/");
      if (pInd === -1) {
         return uri;
      }
      var mInd = pInd + "/dc/type/".length;
      return uri.substring(pInd, uri.indexOf('/', mInd));
   }


// TODO rm OBSOLETE
function toolifyDcResourceJson(prettyDcResourceJson) {
   prettyDcResourceJson = prettyDcResourceJson.replace(/\"http:\/\/data\.oasis-eu\.org\/dc\/type\/([^\/]+)\/([^\"]+)\"/g,
      '"http://data.oasis-eu.org/dc/type/'
      + '<a href="/dc/type/$1" class="dclink" onclick="'
      + 'javascript:return findDataByType($(this).attr(\'href\'));'
      + '">$1</a>'
      + '/'
      + '<a href="/dc/type/$1/$2" class="dclink" onclick="'
   	  + 'javascript:return getData($(this).attr(\'href\'));'
      + '">$2</a>"');
   // for Models only :
   prettyDcResourceJson = prettyDcResourceJson.replace(/\"dcmf:resourceType\": \"([^\"]+)\"/g,
      '"dcmf:resourceType": "'
      + '<a href="/dc/type/$1" class="dclink" onclick="'
      + 'javascript:return findDataByType($(this).attr(\'href\'));'
      + '">$1</a>"');
   return prettyDcResourceJson;
}
function lineBreak(depth) {
	var res = '\n'; // \n OR <br> but not both because in pre code
	for (var i = 0; i < depth; i++) {
		res += '   '; // or \t
	}
	return res;
}
// modelType is where queries are made in, therefore it's the upper resource's
// keyPathInResource is used to make queries in it
var nativeFieldNames = {
      "@id" : null, "o:version" : null, "@type" : null, "dc:created" : null,
      "dc:creator" : null, "dc:modified" : null, "dc:contributor" : null
}
function toolifyDcResourceFieldAndColon(value, key, modelType, upperResource, keyPathInResource) {
   if (typeof nativeFieldNames[key] !== 'undefined' // skip native fields
      || typeof upperResource === 'undefined') { // skip when hashmap of resources
      return JSON.stringify(key, null, '\t') + " : ";
   }
   return '"<a href="/dc/type/dcmo:model_0/' + modelType + '" class="dclink" onclick="'
      + 'javascript:return getData($(this).attr(\'href\'));'
      + '">' + key + '</a>"'
      + '<a href="/dc/type/' + modelType + '?' + keyPathInResource.join('.') + '=' + value + '" class="dclink" onclick="'
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
         modelType = getModelTypeFromUri(resource["@id"]);
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
   return '"' + value.replace(/^http:\/\/data\.oasis-eu\.org\/dc\/type\/([^\/]+)\/(.+)$/g,
         dcConf.containerUrl + '/dc/'
         + '<a href="/dc/type/dcmo:model_0/$1" class="dclink" onclick="'
         + 'javascript:return getData($(this).attr(\'href\'));'
         + '">type</a>'
         + '/'
         + '<a href="/dc/type/$1" class="dclink" onclick="'
         + 'javascript:return findDataByType($(this).attr(\'href\'));'
         + '">$1</a>'
         + '<a href="/dc/type/dcmo:model_0?dcmo:fields.dcmf:resourceType=$1" class="dclink" onclick="'
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
			return '"<a href="/dc/type/' + value + '" class="dclink" onclick="'
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
function setUrl(relativeUrl, dontUpdateDisplay) {
   if (!dontUpdateDisplay && doUpdateDisplay) {
   if (!relativeUrl || relativeUrl === "") {
      $('.myurl').val('');
      document.getElementById('mydata').innerHTML = '';
   } else {
      $('.myurl').val(getModelTypeUrl(relativeUrl));
      document.getElementById('mydata').innerHTML = '...';
   }
   }
   return false;
}
function setError(errorMsg) {
   if (doUpdateDisplay) {
	document.getElementById('mydata').innerHTML = errorMsg;
   }
	return false;
}

// optional : success, error, start (else 0, max 500), limit (else 10 !!! max 100 !)
function findDataByType(relativeUrl, success, error, start, limit) {
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
   dcApi.dc.findDataInType(swaggerParams,
      function(data) {
         var resResources = displayJsonListResult(data, success);
         if (success) {
            success(resResources, relativeUrl);
         }
      }, function(data) {
         setError(data._body._body);
         if (error) {
            error(data, relativeUrl);
         }
      });
   return false;
}
function getData(relativeUrl, success, error) {
   if (typeof relativeUrl === 'string') {
      // NB. modelType encoded as URIs should be, BUT must be decoded before used as GET URI
      // because swagger.js re-encodes (for resourceId, per path element because __unencoded__-prefixed per hack)
      relativeUrl = parseUri(relativeUrl);
   }
   setUrl(relativeUrl, success);
   dcApi.dc.getData({type:relativeUrl.modelType, __unencoded__iri:relativeUrl.id,
         'If-None-Match':-1, Authorization:getAuthHeader()},
      function(data) {
         var resResource = displayJsonObjectResult(data, success);
         if (success) {
        	   success(resResource, relativeUrl);
         }
      }, function(data) {
         setError(data._body._body);
         if (error) {
        	   error(data, relativeUrl);
         }
      });
   return false;
}
function findData(relativeUrl, success, error) {
   if (typeof relativeUrl === 'string') {
      relativeUrl = parseUri(relativeUrl);
   }
   if (relativeUrl.query !== null || relativeUrl.id === null) {
		return findDataByType(relativeUrl, success, error);
	}
	return getData(relativeUrl, success, error);
}
//optional : success, error, start (else 0, max 500), limit (else 10 !!! max 100 !)
function findDataByTypeRdf(relativeUrl, success, error, start, limit) {
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
   dcApi.dc.findDataInType(swaggerParams, {responseContentType:'text/x-nquads'},
      function(data) {
         displayTextResult(data, success);
         if (success) {
            success(data.content.data, relativeUrl);
         }
      }, function(data) {
         setError(data._body._body);
         if (error) {
            error(data); // , resources // NB. always undefined !
         }
      });
   return false;
}


//////////////////////////////////////////////////:
// WRITE

// resources can be a single resource or an array
function postAllDataInType(relativeUrl, resources, success, error) {
   if (typeof relativeUrl === 'string') {
      // NB. modelType encoded as URIs should be, BUT must be decoded before used as GET URI
      // because swagger.js re-encodes
      relativeUrl = parseUri(relativeUrl);
   }
   setUrl(relativeUrl, success);
   if (!resources instanceof Array) { // single resource
      resources = [ resources ];
   }
   dcApi.dc.postAllDataInType({type:relativeUrl.modelType, body:JSON.stringify(resources, null, null),
         Authorization:getAuthHeader()},
      function(data) {
         var resResources = displayJsonListResult(data, success);
         if (success) {
    	     success(resResources, resources);
         }
      },
      function(data) {
         setError(data._body._body);
         if (error) {
            error(data); // , resources // NB. always undefined !
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

function deleteDataInType(resource, success, error) {
   // NB. modelType encoded as URIs should be, BUT must be decoded before used as GET URI
   // because swagger.js re-encodes (for resourceId, per path element because __unencoded__-prefixed per hack)
   var parsedUri = parseUri(resource["@id"]);
   setUrl(parsedUri, success);
   dcApi.dc.deleteData({type:parsedUri.modelType, __unencoded__iri:parsedUri.id,
         'If-Match':resource["o:version"], Authorization:getAuthHeader()},
      function(data) {
         var resResource = displayJsonObjectResult(data, success);
         if (success) {
            success(resResource, resource);
         }
      },
      function(data) {
         setError(data._body._body);
         if (error) {
            error(data); // , resources // NB. always undefined !
         }
      });
   return false;
}

//resources can be a single resource or an array
function postAllData(resources, success, error) {
   if (!resources instanceof Array) { // single resource
      resources = [ resources ];
   }
   dcApi.dc.postAllData({body:JSON.stringify(resources, null, null),
         Authorization:getAuthHeader()},
      function(data) {
         var resResources = displayJsonListResult(data, success);
         if (success) {
            success(resResources, resources);
         }
      }, function(data) {
         setError(data._body._body);
         /*if (data._body._body.indexOf("already existing") != -1) { // TODO better
            findDataByType(relativeUrl, callback);
         }*/
         if (error) {
            error(data); // , resources // NB. always undefined !
         }
      });
    return false;
}


///////////////////////
// CALLBACKS

var doUpdateDisplay = true;

function displayTextResult(data, dontUpdateDisplay) {
   if (!dontUpdateDisplay && doUpdateDisplay) {
   var prettyText = data.content.data;
   $('.mydata').text(prettyText);
   }
}

function displayJsonObjectResult(data, dontUpdateDisplay) {
   var resource = eval('[' + data.content.data + ']')[0];
   if (!dontUpdateDisplay && doUpdateDisplay) {
   var prettyJson = toolifyDcResource(resource, 0); // ,  getModelTypeFromUri(data.request.path) // , upperResource, keyPathInResource
   //var prettyJson = JSON.stringify(resource, null, '\t').replace(/\n/g, '<br>');
   //prettyJson = toolifyDcResourceJson(prettyJson);
   $('.mydata').html(prettyJson);
   }
   return resource;
}

function displayJsonListResult(data, dontUpdateDisplay) {
   var resResources = eval(data.content.data);
   if (!dontUpdateDisplay && doUpdateDisplay) {
   var prettyJson = toolifyDcList(resResources, 0, null, getModelTypeFromUri(data.request.path));

   // adding "..." link for pagination :
   var start = 0;
   var limit = dcConf.queryDefaultLimit; // from conf
   var query = '';
   if (data.request._query) {
      // at least must get actual limit to know whether there might be more data
      // but it's easier to also get start and build query at the same time
      var splitQuery = data.request._query.split('&');
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
      var relativeUrl = data.request.path + '?start=' + previousStart + '&limit=' + limit + query;
      prettyJson = '<a href="' + relativeUrl + '" class="dclink" onclick="'
            + 'javascript:return findDataByType($(this).attr(\'href\'));'
            + '">...</a>' + lineBreak(0) + prettyJson;
   }
   if (resResources.length === limit) {
      var nextStart = start + limit;
      var relativeUrl = data.request.path + '?start=' + nextStart + '&limit=' + limit + query;
      prettyJson += lineBreak(0) + '<a href="' + relativeUrl + '" class="dclink" onclick="'
            + 'javascript:return findDataByType($(this).attr(\'href\'));'
            + '">...</a>';
   }
   ///var prettyJson = JSON.stringify(resResources, null, '\t').replace(/\n/g, '<br>');
   ///prettyJson = toolifyDcResourceJson(prettyJson);
   $('.mydata').html(prettyJson);
   }
   return resResources;
}

function findLinkedData(resourceUri, linkingModelType) {
   var parsedUri = parseUri(resourceUri);
   if (typeof linkingModelType === 'undefined') {
      linkingModelType = parsedUri.modelType;
   }
   var isModel = parsedUri.modelType.indexOf('dcmo:model_') === 0;
   if (isModel) { // lookup instances in this model :
      findDataByType("/dc/type/" + parsedUri.id); // NB. works even if not storage thanks to polymorphism
   } else { // lookup models linking to this one and display them and their resources :
      findDataByType("/dc/type/dcmo:model_0?dcmo:fields.dcmf:resourceType=" + linkingModelType,
            function(modelResources) {
         getData(parsedUri, function(linkedResource) {
            displayModelAndResourceLinks(modelResources, linkedResource, parsedUri, linkingModelType);
         });
      });
      // NB. for subresources : models having this one as mixin can be found by ":" on @type
      // TODO LATER also on dcmo:fields.dcmf:mapFields.dcmf:resourceType for top level maps' fields...
   }
   return false;
}
// displays linking models and their resources
function displayModelAndResourceLinks(linkingModels, linkedResource, parsedUri, linkingModelType) {
   if (typeof linkingModelType === 'undefined') {
      linkingModelType = parsedUri.modelType;
   }
   var html = '';
   if (linkingModels.length === 0) {
      html = 'No model links through '
         + '<a href="/dc/type/dcmo:model_0/' + linkingModelType + '" class="dclink" onclick="'
         + 'javascript:return getData($(this).attr(\'href\'));'
         + '">' + linkingModelType + '</a>'
         + ' at top level to ' + lineBreak(1) + toolifyDcResourceUri(parsedUri.uri) + ' .';
   } else {
      html = 'There are ' + linkingModels.length + ' model(s) linking through '
         + '<a href="/dc/type/dcmo:model_0/' + linkingModelType + '" class="dclink" onclick="'
         + 'javascript:return getData($(this).attr(\'href\'));'
         + '">' + linkingModelType + '</a>'
         + ' at top level to ' + lineBreak(1) + toolifyDcResourceUri(parsedUri.uri) + ' :'; // NB. line break is below
      for (var mInd in linkingModels) {
         var linkingModel = linkingModels[mInd];
         var field = findResource(linkingModel['dcmo:fields'], { 'dcmf:resourceType' : linkingModelType });
         var modelType = linkingModel['dcmo:name'];
         html += lineBreak(0) + '- ';
         html += '<a href="/dc/type/dcmo:model_0/' + modelType + '" class="dclink" onclick="'
            + 'javascript:return getData($(this).attr(\'href\'));'
            + '">' + modelType + '</a>'
         html += ' - resources : ';
         //html += toolifyDcResourceValue(model, null, 1);
         html += dcConf.containerUrl + '/dc/'
            + '<a href="/dc/type/dcmo:model_0/' + modelType + '" class="dclink" onclick="'
            + 'javascript:return getData($(this).attr(\'href\'));'
            + '">type</a>'
            + '/'
            + '<a href="/dc/type/' + modelType + '?' + field['dcmf:name'] + '=' + parsedUri.uri + '" class="dclink" onclick="'
            + 'javascript:return findDataByType($(this).attr(\'href\'));'
            + '">' + modelType + '</a>'
            + '...';
      }
   }
   html += displayModelMixinLinks(linkedResource, parsedUri, linkingModelType);
   $('.mydata').html(html);
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
         html +=  '<a href="/dc/type/dcmo:model_0?dcmo:fields.dcmf:resourceType=' + mixinName + '" class="dclink" onclick="'
            + 'javascript:return findLinkedData(\'/dc/type/' +  parsedUri.modelType + '/' +  parsedUri.id + '\', \'' + mixinName + '\');'
            + '">' + mixinName + '</a>'
            + "...";
      }
   }
   return html;
}


///////////////////////
// MODEL MANIPULATION

function findResource(resources, criteria) {
   for (var rInd in resources) {
      var resource = resources[rInd];
      var found = true;
      for (var key in criteria) {
         if (resource[key] !== criteria[key]) {
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
