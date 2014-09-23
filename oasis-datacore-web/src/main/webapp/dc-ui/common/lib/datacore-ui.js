//var containerUrl = "http://data.oasis-eu.org/"; // rather auto defined in description.html

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
function toolifyDcResourceFieldAndColon(value, key, modelType, resource) {
   if ("@id" == key || "o:version" == key || "@type" == key
         || "dc:created" == key || "dc:creator" == key || "dc:modified" == key || "dc:contributor" == key) {
      // skip
      return JSON.stringify(key, null, '\t') + " : ";
   }
   return '"<a href="/dc/type/dcmo:model_0/' + modelType + '" class="dclink" onclick="'
      + 'javascript:return getData($(this).attr(\'href\'));'
      + '">' + key + '</a>"'
      + '<a href="/dc/type/' + modelType + '?' + key + '=' + value + '" class="dclink" onclick="'
      + 'javascript:return findDataByType($(this).attr(\'href\'));'
      + '"> : </a>';
}
function toolifyDcResource(resource, depth) { // or map
   if (resource == null) {
      return 'null'; // in case of getData() (else done in ...Values)
   }
   var modelType;
   var resourceTypes = resource["@type"];
   if (resourceTypes instanceof Array && resource.length != 0) {
	   modelType = resource["@type"][0];
   } else {
	   modelType = null;
   }
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
	   //resource[key] = toolifyDcResourceValue(value, key, modelType, resource);
      res += toolifyDcResourceFieldAndColon(value, key, modelType, resource)
            + toolifyDcResourceValue(value, key, modelType, resource, subDepth);
   }
   if (!first) {
	   // at least one (should !)
	   res += lineBreak(depth);
   }
   res += '}';
   return res;
   //return resource;
}
function toolifyDcList(values, key, modelType, resource, depth) {
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
 	   res += toolifyDcResourceValue(value, key, modelType, resource, subDepth);
   }
   if (!first) {
	   // at least one
	   res += lineBreak(depth);
   }
   res += ']';
   return res;
   //return resource;
}
function toolifyDcResourceValue(value, key, modelType, resource, depth) {
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
		return '"' + value.replace(/^http:\/\/data\.oasis-eu\.org\/dc\/type\/([^\/]+)\/(.+)$/g,
			      'http://data.oasis-eu.org/dc/'
			      + '<a href="/dc/type/dcmo:model_0/$1" class="dclink" onclick="'
			      + 'javascript:return getData($(this).attr(\'href\'));'
			      + '">type</a>'
			      + '/'
			      + '<a href="/dc/type/$1" class="dclink" onclick="'
			      + 'javascript:return findDataByType($(this).attr(\'href\'));'
			      + '">$1</a>'
			      + '/'
			      + '<a href="/dc/type/$1/$2" class="dclink" onclick="'
			   	  + 'javascript:return getData($(this).attr(\'href\'));'
			      + '">$2</a>') + '"';
	} else if (valueType == 'object') {
		if (value instanceof Array) {
			return toolifyDcList(value, key, modelType, resource, depth);
		} else {
	 	   return toolifyDcResource(value, depth);
		}
	} // 'number', 'boolean'(?) : nothing to do ; TODO others (date) ??
	return JSON.stringify(value, null, '\t');
}
function toolifyDcListOrResource(valuesOrResource) {
   if (typeof valuesOrResource.length !== 'undefined') { // array (=== 'object not enough)
      return toolifyDcList(valuesOrResource, null, null, null, 0);
   } else {
      return toolifyDcResource(valuesOrResource, 0);
   }
}
function setUrl(relativeUrl) {
   if (relativeUrl == null || relativeUrl == "") {
      $('.myurl').val('');
      document.getElementById('mydata').innerHTML = '';
   } else {
      $('.myurl').val(relativeUrl);
      document.getElementById('mydata').innerHTML = '...';
   }
   return false;
}
function setError(errorMsg) {
	document.getElementById('mydata').innerHTML = errorMsg;
	return false;
}
function findDataByType(relativeUrl, success, error) {
   var resourceTypeAndQuery = relativeUrl.replace(/^\/*dc\/type\/*/, "");
   var cleanedRelativeUrl = "/dc/type/" + resourceTypeAndQuery; // also works if no dc/type in relativeUrl
   setUrl(cleanedRelativeUrl);
   var i = resourceTypeAndQuery.indexOf("?");
   // decodeURIComponent
   var modelType = i == -1 ? resourceTypeAndQuery : resourceTypeAndQuery.substring(0, i);
   var query = i == -1 ? "" : resourceTypeAndQuery.substring(i + 1);
   /*var l = '/dc/type/'.length;
   var tq = relativeUrl.substring(l);
   var i = tq.indexOf('?');
   window.t = i == -1 ? tq : tq.substring(0, i);
   window.q = i == -1 ? '' : tq.substring(i + 1);*/
   dcApi.dc.findDataInType({type:modelType, '#queryParameters':query,
         Authorization:'Basic YWRtaW46YWRtaW4='},
      function(data) {
         var resources = eval(data.content.data);
         var prettyJson = toolifyDcList(resources, null, null, null, 0);
         ///var prettyJson = JSON.stringify(resources, null, '\t').replace(/\n/g, '<br>');
         ///prettyJson = toolifyDcResourceJson(prettyJson);
         $('.mydata').html(prettyJson);
         if (success) {
            success(resources);
         }
      }, function(data) {
         setError(data._body._body);
         if (error) {
            error(data);
         }
      });
   /*window.datacore = new SwaggerApi({ url: '/api-docs',
      success: function() {
         if(datacore.ready === true) {
            datacore.apis.dc.findDataInType({type:window.t, '#queryParameters':window.q,
            	   Authorization:'Basic YWRtaW46YWRtaW4='}, function(data) {
               var prettyJson = JSON.stringify(eval(data.content.data), null, '\t').replace(/\n/g, '<br>');
               $('.mydata').html(prettyJson);
            });
         }
      }
   });*/
   return false;
}
function getData(relativeUrl, success, error) {
   var resourceIri = relativeUrl.replace(/^\/*dc\/type\/*/, "");
   var cleanedRelativeUrl = "/dc/type/" + resourceIri; // also works if no dc/type in relativeUrl
   setUrl(cleanedRelativeUrl);
   // decodeURIComponent
   var modelType = resourceIri.substring(0, resourceIri.indexOf("/"));
   var resourceId = resourceIri.substring(resourceIri.indexOf("/") + 1);
   /*var l = '/dc/type/'.length;
   var ti = relativeUrl.substring(l);
   var i = ti.indexOf('/', l);
   window.t = ti.substring(0, i);
   window.iri = ti.substring(i + 1);*/
   dcApi.dc.getData({type:modelType, __unencoded__iri:resourceId,
         'If-None-Match':-1, Authorization:'Basic YWRtaW46YWRtaW4='},
      function(data) {
         var resource = eval('[' + data.content.data + ']')[0];
         var prettyJson = toolifyDcResource(resource, 0);
         //var prettyJson = JSON.stringify(resource, null, '\t').replace(/\n/g, '<br>');
         //prettyJson = toolifyDcResourceJson(prettyJson);
         $('.mydata').html(prettyJson);
         if (success) {
        	 success(resource);
         }
      }, function(data) {
         setError(data._body._body);
         if (error) {
        	 error(data);
         }
      });
   /*window.datacore = new SwaggerApi({ url: '/api-docs', success: function() {
      if(datacore.ready === true) {
         datacore.apis.dc.getData({type:window.t, __unencoded__iri:window.iri, 'If-None-Match':-1,
            Authorization:'Basic YWRtaW46YWRtaW4='}, function(data) {
               var prettyJson = JSON.stringify(eval('[' + data.content.data + ']'), null, '\t').replace(/\n/g, '<br>');
               $('.mydata').html(prettyJson);
            });
         }
      }
   });*/
   return false;
}
function findData(relativeUrl, success, error) {
	if (relativeUrl.indexOf('?') != -1
			|| relativeUrl.replace(/^\/*dc\/type\/*/, "").indexOf('/') == -1) { // none (or last position(s) ??)
		return findDataByType(relativeUrl, success, error);
	}
	return getData(relativeUrl, success, error);
}
function findDataByTypeRdf(relativeUrl, success, error) {
   var resourceTypeAndQuery = relativeUrl.replace(/^\/*dc\/type\/*/, "");
   var cleanedRelativeUrl = "/dc/type/" + resourceTypeAndQuery; // also works if no dc/type in relativeUrl
   setUrl(cleanedRelativeUrl);
   var i = resourceTypeAndQuery.indexOf("?");
   // decodeURIComponent
   var modelType = i == -1 ? resourceTypeAndQuery : resourceTypeAndQuery.substring(0, i);
   var query = i == -1 ? "" : resourceTypeAndQuery.substring(i + 1);
   /*var l = '/dc/type/'.length;
   var tq = relativeUrl.substring(l);
   var i = tq.indexOf('?');
   window.t = i == -1 ? tq : tq.substring(0, i);
   window.q = i == -1 ? '' : tq.substring(i + 1);*/
   dcApi.dc.findDataInType({type:modelType, '#queryParameters':query,
         Authorization:'Basic YWRtaW46YWRtaW4='}, {responseContentType:'text/x-nquads'},
      function(data) {
         var prettyText = data.content.data;
         $('.mydata').text(prettyText);
         if (success) {
            success(data.content.data);
         }
      }, function(data) {
         setError(data._body._body);
         if (error) {
            error(data);
         }
      });
   /*window.datacore = new SwaggerApi({ url: '/api-docs', success: function() {
      if(datacore.ready === true) {
         datacore.apis.dc.findDataInType({type:window.t, '#queryParameters':window.q,
            Authorization:'Basic YWRtaW46YWRtaW4='}, {responseContentType:'text/x-nquads'}, function(data) {
               var prettyText = data.content.data; $('.mydata').text(prettyText);
            });
         }
      }
   });*/
   return false;
}


//////////////////////////////////////////////////:
// WRITE

function postAllDataInType(relativeUrl, resources, success, error) {
   var modelType = relativeUrl.replace(/^\/*dc\/type\/*/, "");
   var cleanedRelativeUrl = "/dc/type/" + modelType; // also works if no dc/type in relativeUrl
   setUrl(cleanedRelativeUrl);
   // decodeURIComponent
   /*var l = '/dc/type/'.length;
   var tq = relativeUrl.substring(l);
   var i = tq.indexOf('?');
   window.t = i == -1 ? tq : tq.substring(0, i);
   window.q = i == -1 ? '' : tq.substring(i + 1);*/
   dcApi.dc.postAllDataInType({type:modelType, body:resources,
         Authorization:'Basic YWRtaW46YWRtaW4='},
      function(data) {
         var resResources = eval(data.content.data);
         var prettyJson = toolifyDcList(resResources, null, null, null, 0);
         ///var prettyJson = JSON.stringify(resResources, null, '\t').replace(/\n/g, '<br>');
         ///prettyJson = toolifyDcResourceJson(prettyJson);
         $('.mydata').html(prettyJson);
         if (success) {
    	     success(resources);
         }
      },
      function(data) {
         setError(data._body._body);
         if (error) {
            error(data);
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
   var uri = resource["@id"];
   var resourceIri = uri.replace(/^\/*dc\/type\/*/, "");
   var cleanedRelativeUrl = "/dc/type/" + resourceIri; // also works if no dc/type in relativeUrl
   setUrl(cleanedRelativeUrl);
   // decodeURIComponent
   var modelType = resourceIri.substring(0, resourceIri.indexOf("/"));
   var resourceId = resourceIri.substring(resourceIri.indexOf("/") + 1);
   /*var relativeUrl = uri.substring(uri.indexOf("/dc/type/"));
   setUrl(relativeUrl);
   var l = '/dc/type/'.length;
   var ti = relativeUrl.substring(l);
   var i = ti.indexOf('/', l);
   window.t = ti.substring(0, i);
   window.iri = ti.substring(i + 1);*/
   dcApi.dc.deleteData({type:modelType, __unencoded__iri:resourceId,
         'If-Match':resource["o:version"], Authorization:'Basic YWRtaW46YWRtaW4='},
      function(data) {
         var resource = eval('[' + data.content.data + ']')[0];
         var prettyJson = toolifyDcResource(resource, 0);
         //var prettyJson = JSON.stringify(resource, null, '\t').replace(/\n/g, '<br>');
         //prettyJson = toolifyDcResourceJson(prettyJson);
         $('.mydata').html(prettyJson);
         if (success) {
            success(resource);
         }
      },
      function(data) {
         setError(data._body._body);
         if (error) {
            error(data);
         }
      });
   return false;
}

function postAllData(resources, success, error) {
   dcApi.dc.postAllData({body:resources,
         Authorization:'Basic YWRtaW46YWRtaW4='},
      function(data) {
         var resResources = eval(data.content.data);
         var prettyJson = toolifyDcList(resResources, null, null, null, 0);
         ///var prettyJson = JSON.stringify(resResources, null, '\t').replace(/\n/g, '<br>');
         ///prettyJson = toolifyDcResourceJson(prettyJson);
         $('.mydata').html(prettyJson);
         if (success) {
            success(resources);
         }
      }, function(data) {
         setError(data._body._body);
         /*if (data._body._body.indexOf("already existing") != -1) { // TODO better
            findDataByType(relativeUrl, callback);
         }*/
         if (error) {
            error(data);
         }
      });
    return false;
 }