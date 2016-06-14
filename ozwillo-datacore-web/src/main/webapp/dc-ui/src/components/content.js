import React from 'react';
import { connect } from 'react-redux';
import * as actions from '../actions/actionIndex.js';
import CodeView from './codeView.js';


export class Content extends React.Component{
  constructor(props) {
    super(props);
    this.state = {
      errorMessage: false,
      messageError: "",
    };
  }

  setCurrentState = (state) => {
    this.setState(state);
  }

  preventClickEffect = () => {
    $('.dclink').click((e) => {
      e.preventDefault();
    });
    $('.playground').click((e) => {
      e.preventDefault();
    });
  }

  componentDidMount = () => {
    $("#listButton").popup();
    $("#dcButton").popup();
    $("#debugButton").popup();
    $("#RDFButton").popup();
    $("#editButton").popup();
    $("#delButton").popup();
    $("#MButton").popup();
    $("#HButton").popup();
    $("#postButton").popup();
    $("#putButton").popup();

    $("#postButton").hide();
    $("#putButton").hide();

    //we expose this function in order to access it into datacore-ui.js
    window.functionExposition = this;
    this.preventClickEffect();
  }

  componentDidUpdate = () => {
    this.preventClickEffect();
  }

  getButton = () => {
    this.callAPIUpdatePlayground(this.props.currentPath)
  }

  listButton = () => {
    var relativeUrl = this.props.currentPath;
    this.ajaxCall(
      relativeUrl,
      (data) => {
        this.setUrl(relativeUrl, null);

        if (Object.prototype.toString.call( data ) === '[object Array]' ){
          var list_data = [];
          for (var resource of data){
            list_data.push({"@id" : resource["@id"]});
          }
          var resResourcesOrText = displayJsonListResult(list_data, relativeUrl);
        }
        else{
          data = {"@id" : data["@id"]};
          var resResourcesOrText = displayJsonObjectResult(data);
        }
        this.props.dispatch(actions.setCurrentDisplay(resResourcesOrText));
      },
      () => {
        this.setState({errorMessage: true, messageError:"Error accessing the current path"});
      }
    );
  }

  extractDc = (resource) => {
    return {
      "@id": resource["@id"],
      "o:version": resource["o:version"],
      "@type": resource["@type"],
      "dc:created": resource["dc:created"],
      "dc:modified": resource["dc:modified"],
      "dc:creator": resource["dc:creator"],
      "dc:contributor": resource["dc:contributor"],
      "o:partial": resource["o:partial"],
    };

  }

  dcButton = () => {
    var relativeUrl = this.props.currentPath;
    this.ajaxCall(
      relativeUrl,
      (data) => {
        this.setUrl(relativeUrl, null);

        if (Object.prototype.toString.call( data ) === '[object Array]' ){
          var list_data = [];
          for (var resource of data){
            list_data.push(this.extractDc(resource));
          }
          var resResourcesOrText = displayJsonListResult(list_data, relativeUrl);
        }
        else{
          data = this.extractDc(data);
          var resResourcesOrText = displayJsonObjectResult(data);
        }
        this.props.dispatch(actions.setCurrentDisplay(resResourcesOrText));
      },
      () => {
        this.setState({errorMessage: true, messageError: "Error accessing the current path"});
      }
    );
  }

  debugButton = () => {
    var relativeUrl = this.props.currentPath;
    this.ajaxCall(
      relativeUrl,
      (data) => {
        this.setUrl(relativeUrl, null);
        var resResourcesOrText = displayJsonObjectResult(data.debug);
        this.props.dispatch(actions.setCurrentDisplay(resResourcesOrText));
      },
      () => {
        this.setState({errorMessage: true, messageError:"Error accessing the current path"});
      },
      {
        "X-Datacore-Debug": true
      }
    );
  }

  modelButton = () => {
    var url = this.props.currentPath;
    var beginSearch = url.indexOf("type/")+5;

    var modelName = "";
    for(var i=beginSearch; i<=url.length; i++){
      modelName = url.substring(beginSearch,i);
      if(url[i] === "/" || url[i] === "?"){
        break;
      }
    }

    var relativeUrl = buildRelativeUrl("dcmo:model_0/"+modelName);

    this.ajaxCall(
      relativeUrl,
      (data) => {
        this.setUrl(relativeUrl, null);
        var resResourcesOrText = displayJsonObjectResult(data);
        this.props.dispatch(actions.setCurrentDisplay(resResourcesOrText));
      },
      () => {
        this.setState({errorMessage: true, messageError: "Error accessing the current path"});
      }
    );
  }

  RDFButton = () => {
    var relativeUrl = this.props.currentPath;
    this.ajaxCall(
      relativeUrl,
      (data) => {
        this.setUrl(relativeUrl, null);
        this.props.dispatch(actions.setCurrentDisplayRDF(data));
      },
      () => {
        this.setState({errorMessage: true, messageError: "Error accessing the current path"});
      },
      {'Accept':'text/x-nquads'}
    );
  }

  deleteButton = () => {
    var relativeUrl = this.props.currentPath;

    //we first make a get request to get the current version of the object
    this.ajaxCall(
      relativeUrl,
      (data) => {
        this.setUrl(relativeUrl, null);

        //we call the delete function
        if (Object.prototype.toString.call( data ) === '[object Array]'){
          for (var resource of data){
            var currentVersion = resource["o:version"];
            this.deleteRessource(relativeUrl, currentVersion);
          }
        }
        else{
          var currentVersion = data["o:version"];
          this.deleteRessource(relativeUrl, currentVersion);
        }
      },
      () => {
        this.setState({errorMessage: true, messageError: "Can't delete this data"});
      },
      null,
      'GET'
    );
  }

  deleteRessource = (relativeUrl, version) => {
    this.ajaxCall(
      relativeUrl,
      (data) => {
        this.setUrl(relativeUrl, null);
        this.props.dispatch(actions.setCurrentDisplay(data));
      },
      () => {
        this.setState({errorMessage: true});
      },
      {"If-match": version},
      'DELETE'
    );
  }

  editButton = () => {
    //we check if the currentJson is corresponding to a single resource
    if (this.props.currentJson.charAt(0) === "{"){
      var relativeUrl = this.props.currentPath;

      //we make a call in order to get the datas without the html formating
      this.ajaxCall(
        relativeUrl,
        (data) => {
          this.setUrl(relativeUrl, null);
          this.props.dispatch(actions.setEditable(data));
        },
        () => {
          this.setState({errorMessage: true, messageError:""});
        },
        null,
        'GET'
      );

      $("#transitionEditButton").width(148);
      $('#editButton').transition({animation: 'scale', duration: 150});
      setTimeout(() => {$('#postButton').transition({animation: 'scale', duration: 150})}, 200);
      setTimeout(() => {$('#putButton').transition({animation: 'scale', duration: 150})}, 200);
    }
    else{
      this.setState({errorMessage: true, messageError:"You can't edit an array in this playground, please select a single Resource"});
    }
  }

  putButton = () => {
    this.putOrPostButton("PUT");
  }

  postButton = () => {
    this.putOrPostButton("POST");
  }

  putOrPostButton = (requestType) => {
    var relativeUrl = this.props.currentPath;
    var reactParent = this;
    this.ajaxCall(
      "/dc",
      (data) => {
        this.setUrl(relativeUrl, null);
        var resResourcesOrText = displayJsonObjectResult(data);

        reactParent.props.dispatch(actions.setCurrentDisplay(resResourcesOrText));

        setTimeout(() => {$("#transitionEditButton").width(69)},200);
        $('#postButton').transition({animation: 'scale', duration: 150});
        $('#putButton').transition({animation: 'scale', duration: 150});
        setTimeout(() => {$('#editButton').transition({animation: 'scale', duration: 150})}, 200);
      },
      () => {
        this.setState({errorMessage: true, messageError:"Impossible to Post or Put datas"});
      },
      {"Content-Type": "application/json"},
      requestType,
      this.props.currentJson
    );
  }

  setUrl = (relativeUrl) => {
    this.props.dispatch(actions.setCurrentQueryPath(relativeUrl));
  }

  ajaxCall = (relativeUrl, currentSuccess, currentError, additionalHeaders, operation, data) => {
    var currentOperation = operation !== null ? operation: 'GET';

    var headers = {
      'Authorization' : "Basic YWRtaW46YWRtaW4=",
      'If-None-Match': -1,
      'Accept' : 'application/json',
      'X-Datacore-Project': getProject() //TODO: put in the store the current Project
    };
    headers = $.extend(headers, additionalHeaders);

    $.ajax({
      url: relativeUrl,
      type: currentOperation,
      headers: headers,
      data: data,
      success: currentSuccess,
      error: currentError
    });
  }


  callAPIUpdatePlaygroundOnClick = (event) => {
    this.callAPIUpdatePlayground(event.target.href);
  }

  callAPIUpdatePlayground = (relativeUrl) => {
    //TODO: encode URI
    var reactParent = this;

    this.ajaxCall(
      relativeUrl,
      (data) => {
        this.setUrl(relativeUrl, null);
        //TODO: teste le cas du RDF
        //if the current object is an array, we call displayJsonListResult
        if (Object.prototype.toString.call( data ) === '[object Array]' ){
          var resResourcesOrText = displayJsonListResult(data, relativeUrl);
        }
        else{
          var resResourcesOrText = displayJsonObjectResult(data);
        }
        reactParent.props.dispatch(actions.setCurrentDisplay(resResourcesOrText));
        //success(resResourcesOrText, requestToRelativeUrl(data.request), data, handlerOptions);
      },
      (xhr, ajaxOptions, thrownError) => {
        reactParent.setState({errorMessage: true});
      }
    );
  }


  render() {
    return (
      <div className="twelve wide column ui grid">
        <div className="row">
          <h2 className="ui header">
            <i className="configure icon"></i>
            <div className="content">
              Datacore API
            </div>
          </h2>

        </div>
        <div className="ui grid main">
          <div className="row ui form">
            <div className="field adresseget">
              <div className="ui label pointing below">
                <p>Datacore (https://data.ozwillo-preprod.eu)</p>
              </div>
              <InputCurrentPathConnected />
            </div>
          </div>
          <div className="row ui centered">
            <button className="small ui button" onClick={this.getButton}>GET</button>
            <button className="small ui button" onClick={this.listButton} id="listButton" data-content="List view (minimal)">l</button>
            <button className="small ui button" onClick={this.dcButton} id="dcButton" data-content="List view (Dublin Core Notation)">dc</button>
            <button className="small ui button" onClick={this.debugButton} id="debugButton" data-content="Debug/explain query">?</button>
            <button className="small ui button" onClick={this.RDFButton} id="RDFButton" data-content="RDF N-QUADS representation">RDF</button>
            <div id="transitionEditButton">
              <button className="small ui button" id="editButton" onClick={this.editButton} data-content="Edit data">edit</button>
              <button className="small ui button" id="postButton" onClick={this.postButton} data-content="Post data (merge or create)">POST</button>
              <button className="small ui button" id="putButton" onClick={this.putButton} data-content="Put data (replace existing)">PUT</button>
            </div>
            <button className="small ui button" onClick={this.deleteButton} id="delButton" data-content="Delete data">del</button>
            <button className="small ui button" onClick={this.modelButton} id="MButton" data-content="Go to model">M</button>
            <button className="small ui button" id="HButton" data-content="Previous version if history is enabled">H</button>
          </div>

          {this.state.errorMessage ?
            <MessageErrorPath setParentState={this.setCurrentState} message={this.state.messageError}/>
           : null}

          <CodeView currentJson={this.props.currentJson} codeView={this.props.codeView}/>


          <Reading reading={this.props.reading} callAPIUpdatePlaygroundOnClick={this.callAPIUpdatePlaygroundOnClick}/>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  currentJson: state.currentJson,
  currentPath: state.currentPath,
  codeView: state.codeView
})
export default Content = connect(mapStateToProps)(Content);

export class MessageErrorPath extends React.Component{
  closeMessage = (e) => {
    $('.errorPath').transition('fade');
  }
  componentDidMount = () => {
    var component = this;
    $('.message .close')
    .on('click', function() {
      $(this)
        .closest('.errorPath')
        .transition('fade')
      ;
      component.props.setParentState({errorMessage: false});
    });
  }
  render() {
    return(
      <div className="ui icon error message errorPath">
      <i className="close icon"></i>
      <i className="warning circle icon" onClick={this.closeMessage}></i>
      <p>
        {this.props.message}
      </p>
      </div>
    );
  }
}


export class InputCurrentPath extends React.Component{
  updateCurrentPath = (e) => {
    this.props.dispatch(actions.setCurrentQueryPath(e.target.value));
  }
  render() {
    return (
      <input className="myurl" value={this.props.currentPath} type="text" onChange={this.updateCurrentPath}/>
    );
  }
}
const mapStateToPropsInputCurrentPath = (state/*, props*/) => ({
    currentPath: state.currentPath
})
const InputCurrentPathConnected = connect(mapStateToPropsInputCurrentPath)(InputCurrentPath);


class Reading extends React.Component{
  componentWillUpdate = () => {
    $('.reading').transition('hide');
    $('.reading').transition('fade right');
  }
  render() {
    return (
      <div className="row ui segment reading segmentpadding">
        {/*we pass the function to children in this particular way because of this.props.reading*/}
        {React.cloneElement(this.props.reading, { callAPIUpdatePlaygroundOnClick: this.props.callAPIUpdatePlaygroundOnClick })}
      </div>
    );
  }
}
