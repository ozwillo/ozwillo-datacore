import React from 'react';
import { connect } from 'react-redux';
import * as actions from '../actions/actionIndex.js';

export class Content extends React.Component{
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
    $("#interogationButton").popup();
    $("#RDFButton").popup();
    $("#editButton").popup();
    $("#delButton").popup();
    $("#MButton").popup();
    $("#HButton").popup();
    $("#postButton").popup();
    $("#putButton").popup();

    $("#postButton").hide();
    $("#putButton").hide();

    $('.message .close')
    .on('click', function() {
      $(this).closest('.message').transition('fade');
    });

    //we expose this function in order to acces it into datacore-ui
    window.functionExposition = this;
    this.preventClickEffect();
  }

  componentDidUpdate = () => {
    this.preventClickEffect();
  }

  replaceEdit = () => {
    $("#transitionEditButton").width(148);
    $('#editButton').transition({animation: 'scale', duration: 150});
    setTimeout(() => {$('#postButton').transition({animation: 'scale', duration: 150})}, 200);
    setTimeout(() => {$('#putButton').transition({animation: 'scale', duration: 150})}, 200);
  }

  clickPost = () => {
    setTimeout(() => {$("#transitionEditButton").width(69)},200);
    $('#postButton').transition({animation: 'scale', duration: 150});
    $('#putButton').transition({animation: 'scale', duration: 150});
    setTimeout(() => {$('#editButton').transition({animation: 'scale', duration: 150})}, 200);
  }

  clickPut = () => {
    $("#transitionEditButton").width(69);
    $('#postButton').transition({animation: 'scale', duration: 150});
    $('#putButton').transition({animation: 'scale', duration: 150});
    setTimeout(() => {$('#editButton').transition({animation: 'scale', duration: 150})}, 200);
  }
  callAPIUpdatePlaygroundOnClick = (event) => {
    this.callAPIUpdatePlayground(event.target.href);
  }
  callAPIUpdatePlayground = (relativeUrl) => {
    //TODO: encode URI
    var reactParent = this;

    $.ajax({
      url: relativeUrl,
      type: 'GET',
      headers: {
        "Authorization" : "Basic YWRtaW46YWRtaW4=",
        'Accept' : 'application/json',
        'X-Datacore-Project': getProject() //TODO: put in the store the current Project
      },
      success: function(data) {
        setUrl(relativeUrl, null);

        //TODO: teste le cas du RDF
        var resResourcesOrText = displayJsonListResult(data, relativeUrl, this.headers, this.url);
        reactParent.props.dispatch(actions.setCurrentDisplay(resResourcesOrText));

        //success(resResourcesOrText, requestToRelativeUrl(data.request), data, handlerOptions);
      },
    });
    return false;
  }
  createMarkup= () => {
    return {__html: this.props.currentJson}
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
          <div className="ui icon info message">
            <i className="close icon"></i>
            <i className="warning circle icon"></i>
            <p>
              This playground does not work well on Chrome, use Firefox instead
            </p>
          </div>
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
            <button className="small ui button">GET</button>
            <button className="small ui button" id="listButton" data-content="List view (minimal)">l</button>
            <button className="small ui button" id="dcButton" data-content="List view (Dublin Core Notation)">dc</button>
            <button className="small ui button" id="interogationButton" data-content="Debug/explain query">?</button>
            <button className="small ui button" id="RDFButton" data-content="RDF N-QUADS representation">RDF</button>
            <div id="transitionEditButton">
              <button className="small ui button" id="editButton" onClick={this.replaceEdit} data-content="Edit data">edit</button>
              <button className="small ui button" id="postButton" onClick={this.clickPost} data-content="Post data (merge or create)">POST</button>
              <button className="small ui button" id="putButton" onClick={this.clickPut} data-content="Put data (replace existing)">PUT</button>
            </div>
            <button className="small ui button" id="delButton" data-content="Delete data">del</button>
            <button className="small ui button" id="MButton" data-content="Go to model">M</button>
            <button className="small ui button" id="HButton" data-content="Previous version if history is enabled">H</button>

          </div>
          <div className="row ui segment">
          <pre className="segmentpadding mydata" dangerouslySetInnerHTML={this.createMarkup()}>

          </pre>
          </div>
          <Reading reading={this.props.reading} callAPIUpdatePlaygroundOnClick={this.callAPIUpdatePlaygroundOnClick}/>
        </div>
      </div>
    );
  }
}
//the first argument is an obligation, so there is an offset of -1 with the call
const mapStateToProps = (state) => ({
  currentJson: state.currentJson
})
export default Content = connect(mapStateToProps)(Content);



export class InputCurrentPath extends React.Component{
  updateCurrentPath = (e) => {
    this.props.dispatch(actions.setCurrentQueryPath(e.target.value));
  }
  render() {
    return (
      <input className="myurl" id defaultValue={this.props.currentPath} type="text" onChange={this.updateCurrentPath}/>
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
