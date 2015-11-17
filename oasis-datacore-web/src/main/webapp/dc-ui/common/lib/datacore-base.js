
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
      if (dcConf.localauthdevmode) {
         // log test admin :
         userinfo = {
               nickname : 'Administrator',
               sub_groups : ['admin', 'u_john', 'tutor_jim',
                             'rm_altTourism.place.SofiaMonastery_readers'],
               test : true,
               anonymous : true
         };
      } else {
         // redirect to login :
         window.location.href = "/dc/playground/login"; // .href simulates a link http://stackoverflow.com/questions/1655065/redirecting-to-a-relative-url-in-javascript
      }
      $('#logout').hide();
   }
   var welcomeHtml = 'Welcome <span style="text-decoration:underline;" title="'
      + ((userinfo.sub) ? 'sub: ' + userinfo.sub : 'none') + "\n"
      + ((userinfo.sub_groups) ? 'groups: ' + userinfo.sub_groups.join(', ') : 'no groups')
      + '">'
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
      
      var userinfo = JSON.parse(readCookie("userinfo"));
      if (!userinfo) {
         initUserInfo(); // relog
      }
      var id_client = userinfo['id_token'];
      deleteCookie('authorization');
      deleteCookie('userinfo');
      window.location.assign(dcConf.accountsBaseUrl + '/a/logout?id_token_hint='
            + id_client + '&post_logout_redirect_uri=' + dcConf.baseUrl);
      return ;//if no return don't work window.location.assign
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
   // getting dc playground conf ex. localauthdevmode :
   initDcConf(function(result){
      window.dcConf = JSON.parse(result);
      console.log("conf", dcConf);

      initDcApi();
      completePlaygroundInit();

      initUserInfoUi();
   });
}

function initDcApi() {
   var apiDocsUrl = window.dcConf.apiDocsUrl; // Ozwillo ; OLD "/api-docs", STATIC /dc-ui/api-docs
   var options = {
         url: apiDocsUrl,
         supportedSubmitMethods: ['get', 'post', 'put', 'delete'] // Ozwillo TODO PATCH ?!?
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
   // getting dc playground conf ex. localauthdevmode :
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
   var apiDocsUrl = window.dcConf.apiDocsUrl; // Ozwillo ; OLD "/api-docs", STATIC /dc-ui/api-docs
   /*if (url.indexOf("http") !== 0) {
      // else CORS request to ex. http://localhost/ and "uncaught exception: Please specify the protocol for /api-docs"
      url = buildUrl(window.location.href.toString(), url);
   }*/
   window.swaggerUi = new SwaggerUi({
      url: apiDocsUrl,
      dom_id: "swagger-ui-container",
      supportedSubmitMethods: ['get', 'post', 'put', 'delete'], // Ozwillo TODO PATCH ?!?
      onComplete: function(swaggerApi, swaggerUi){
         if(console) {
            console.log("Loaded SwaggerUI")
         }
         $("input[name='Authorization']").val(getAuthHeader());//FOR use the token in swager
         $("input[name='X-Datacore-Project']").val(getProject());
         window.dcApi = swaggerUi.api; // Ozwillo
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
      docExpansion: "list" // Ozwillo rather than none, full see https://github.com/wordnik/swagger-ui
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
   // getting dc playground conf ex. localauthdevmode :
   $.ajax({
      url:"/dc/playground/configuration",
      headers: { Authorization:getAuthHeader() },
      success: success,
      error: function(jqXHR, textStatus, errorThrown) { // such as when 401 Not Authorized
          window.location.assign('/'); // which will require auth and redirect to login page
      }
   });
}
function initDcConfReplacements() {
   for (var key in dcConf) {
      $(".dcConf_" + key).html("" + dcConf[key]); // converting to string first
      $(".dcConf_" + key + "_link").html('<a href="' + dcConf[key] + '">' + dcConf[key] + '</a>');
   }
}

function completePlaygroundInit() {
   initDcConfReplacements(); // Ozwillo
   $('pre code').each(function(i, e) {hljs.highlightBlock(e)});
}