import React from 'react';
import { connect } from 'react-redux';
import * as actions from '../actions/actionIndex.js';

export default class Content extends React.Component{
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
          <pre className="segmentpadding mydata">

            </pre>
          </div>
          <Reading reading={this.props.reading} />
        </div>
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
      <input className="myurl" id defaultValue={this.props.currentPath} type="text" onChange={this.updateCurrentPath}/>
    );
  }
}

const mapStateToPropsInputCurrentPath = (state/*, props*/) => {
  return {
    currentPath: state.currentPath
  }
}

const InputCurrentPathConnected = connect(mapStateToPropsInputCurrentPath)(InputCurrentPath);

class Reading extends React.Component{
  componentWillUpdate = () => {
    $('.reading').transition('hide');
    $('.reading').transition('fade right');
  }

  render() {
    return (
      <div className="row ui segment reading segmentpadding">

        {this.props.reading}
      </div>
    );
  }
}
