
////////////////////////////////////////////////
// USERINFO LIB

// see http://stackoverflow.com/questions/13353352/use-this-javascript-to-load-cookie-into-variable
function readCookie(key) {
   var result;
   return (result = new RegExp('(?:^|; )' + encodeURIComponent(key) + '=([^;]*)').exec(document.cookie)) ? (result[1]) : null;
}
function deleteCookie(key) {
   document.cookie = encodeURIComponent(key) + '=; Path=/; expires=' + new Date(-1).toGMTString();
}
function getAuthHeader() {
   var authCookie = readCookie("authorization");
   if (authCookie && authCookie.length != 0) {
      return authCookie;
   }
   return 'Basic YWRtaW46YWRtaW4=';
}


////////////////////////////////////////////////
// USERINFO UI

function initUserInfo() {
   var userinfo;
   var userinfoString = readCookie('userinfo');
   if (userinfoString && userinfoString !== null) {
      userinfo = JSON.parse(userinfoString);
      if (!userinfo || userinfo === null) {
         console.log('Unable to parse userinfo ' + userinfoString);
      }
      $('#logout').show();
   } else {
      if (dcConf.devmode) {
         userinfo = {
               nickname : 'Administrator',
               sub_groups : 'admin,u_john,tutor_jim,rm_altTourism.place.SofiaMonastery_readers',
               test : true,
               anonymous : true
         };
      } else {
         userinfo = {
               nickname : 'Anonymous',
               sub_groups : 'guest',
               anonymous : true
         };
      }
      $('#logout').hide();
   }
   var welcomeHtml = 'Welcome <span style="text-decoration:underline;" title="'
      + ((userinfo.sub_groups) ? 'groups: ' + userinfo.sub_groups : 'no groups') + '">'
      + userinfo.nickname + '</span>'
      + ((userinfo.test) ? ' (test account)' : '')
      + ((userinfo.email) ? ' (' + userinfo.email + ')' : '')
      + ((userinfo.anonymous) ? ' - <a href="/dc/playground/login">login</a>' : '');
   $('#userinfo').html(welcomeHtml);
}

function initUserInfoUi() {
   initUserInfo();
   
   $('#logout').on('click', function(e) {
      e.preventDefault();
      deleteCookie('authorization');
      deleteCookie('userinfo');
      initUserInfo();
      ///return false;
   });

   $.ajax({
      url:"/dc/type/dcmp:Project_0?dcmpv:name=%2B&limit=100", // sorted on dcmpv:name (%2B = + ; would allow range pagination)
      headers: { Authorization:getAuthHeader() }, // , 'X-Datacore-View':'dcmpv:PointOfView_0' (any project should do)
      success: function(userProjects) {
         var optionsHtml = '';
         for (var i in userProjects) {
            var projectName = userProjects[i]['dcmpv:name'];
            optionsHtml += '<option onclick="javascript:'
               + 'setProject($(this).attr(\'value\'));return false;" value="'
               + projectName + '">' + projectName;
            var visibleProjectNames = userProjects[i]['dcmp:visibleProjectNames'];
            if (visibleProjectNames && visibleProjectNames.length !== 0) {
               var startsByItself = visibleProjectNames[0] === projectName;
               if (!startsByItself || visibleProjectNames.length !== 1) {
                  var vpnJoined = visibleProjectNames.join(', ');
                  if (startsByItself) {
                     vpnJoined = vpnJoined.substring(projectName.length + 2);
                  }
                  optionsHtml += ' (visible : ' + vpnJoined;
                  var forkedUris = userProjects[i]['dcmp:forkedUris'];
                  if (forkedUris && forkedUris.length !== 0) {
                     optionsHtml += ', forked URIs : ' + forkedUris.join(', ');
                  }
                  optionsHtml += ')';
               }
            }
            optionsHtml += '</option>\n';
         }
         $('#project').html(optionsHtml);
         $('#project').val(getProject());
      }
   });
}


////////////////////////////////////////////////
// PLAYGROUND UI (without Swagger UI)

function initPlayground() {
   // getting dc playground conf ex. devmode :
   initDcConf(function(result){
      window.dcConf = JSON.parse(result);
      console.log("conf", dcConf);

      initDcApi();
      completePlaygroundInit();

      initUserInfoUi();
   });
}

function initDcApi() {
   var apiDocsUrl = window.dcConf.apiDocsUrl; // OASIS ; OLD "/api-docs", STATIC /dc-ui/api-docs
   var options = {
         url: apiDocsUrl,
         supportedSubmitMethods: ['get', 'post', 'put', 'delete'] // OASIS TODO PATCH ?!?
       };
   if (options.url.indexOf("http") !== 0) {
      // else CORS request to ex. http://localhost/ and "uncaught exception: Please specify the protocol for /api-docs"
      options.url = buildUrl(window.location.href.toString(), options.url);
   }
   ///this.headerView.update(options.url);
   var dcApi = new SwaggerApi(options.url, this.options);
   dcApi.build();
   window.dcApi = dcApi;
}

function buildUrl(base, url) {
  var parts;
  console.log("base is " + base);
  parts = base.split("/");
  base = parts[0] + "//" + parts[2];
  if (url.indexOf("/") === 0) {
    return base + url;
  } else {
    return base + "/" + url;
  }
}


////////////////////////////////////////////////
//PLAYGROUND UI - WITH SWAGGER UI

function initPlaygroundWithSwaggerUi() {
   // getting dc playground conf ex. devmode :
   initDcConf(function(result){
      window.dcConf = JSON.parse(result);
      console.log("conf", dcConf);

      initDcApiWithSwaggerUi(function() {
         // adding logo :
         $('#resources').prepend('<div style="padding: 10px 0 20px 40px;"/><div id="header"><a id="logo" href="http://swagger.wordnik.com" class="swagger-ui-wrap">swagger</a></div>');
         
         completePlaygroundInit();
      });

      initUserInfoUi();
   });
}

function initDcApiWithSwaggerUi(success) {
   var apiDocsUrl = window.dcConf.apiDocsUrl; // OASIS ; OLD "/api-docs", STATIC /dc-ui/api-docs
   /*if (url.indexOf("http") !== 0) {
      // else CORS request to ex. http://localhost/ and "uncaught exception: Please specify the protocol for /api-docs"
      url = buildUrl(window.location.href.toString(), url);
   }*/
   window.swaggerUi = new SwaggerUi({
      url: apiDocsUrl,
      dom_id: "swagger-ui-container",
      supportedSubmitMethods: ['get', 'post', 'put', 'delete'], // OASIS TODO PATCH ?!?
      onComplete: function(swaggerApi, swaggerUi){
         if(console) {
            console.log("Loaded SwaggerUI")
         }
         window.dcApi = swaggerUi.api; // OASIS
         if (success) {
            success();
         }
      },
      onFailure: function(data) {
         if(console) {
            console.log("Unable to Load SwaggerUI");
            console.log(data);
         }
      },
      docExpansion: "list" // OASIS rather than none, full see https://github.com/wordnik/swagger-ui
   });
   /*$('#input_apiKey').change(function() {
      var key = $('#input_apiKey')[0].value;
      console.log("key: " + key);
      if(key && key.trim() != "") {
         console.log("added key " + key);
         window.authorizations.add("key", new ApiKeyAuthorization("api_key", key, "query"));
      }
   })*/
   window.swaggerUi.load();
}


////////////////////////////////////////////////
//PLAYGROUND UI - CONF & COMPLETE

function initDcConf(success) {
   // getting dc playground conf ex. devmode :
   $.ajax({
      url:"/dc/playground/configuration",
      headers: { Authorization:getAuthHeader() },
      success: success
         });
}
function initDcConfReplacements() {
   for (var key in dcConf) {
      $(".dcConf_" + key).html("" + dcConf[key]); // converting to string first
      $(".dcConf_" + key + "_link").html('<a href="' + dcConf[key] + '">' + dcConf[key] + '</a>');
   }
}

function completePlaygroundInit() {
   initDcConfReplacements(); // OASIS
   $('pre code').each(function(i, e) {hljs.highlightBlock(e)});
}