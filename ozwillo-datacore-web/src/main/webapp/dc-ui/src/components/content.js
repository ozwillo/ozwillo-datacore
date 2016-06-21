import React from 'react';
import { connect } from 'react-redux';
import * as actions from '../actions/actionIndex.js';

import CodeView from './codeView.js';
import MessageErrorPath from './messageErrorPath.js';
import InputCurrentPath from './inputCurrentPath.js';
import Reading from './reading.js';


import EditButtons from './buttons/editButtons.js';
import GetButton from './buttons/getButton.js';
import ListButton from './buttons/listButton.js';
import DCButton from './buttons/dcButton.js';
import DebugButton from './buttons/debugButton.js';
import RDFButton from './buttons/RDFButton.js';
import DeleteButton from './buttons/deleteButton.js';
import ModelButton from './buttons/modelButton.js';
import HistoricButton from './buttons/historicButton.js';

import {ajaxCall} from '../utils.js';

export class Content extends React.Component{
  constructor(props) {
    super(props);
    this.state = {
      errorMessage: false,
      messageError: "",
      messageErrorDetails: ""
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

  setErrorMessage = (message, messageErrorDetails) => {
    this.setState({errorMessage: true, messageError: message, messageErrorDetails: messageErrorDetails});
  }

  setUrl = (relativeUrl) => {
    this.props.dispatch(actions.setCurrentQueryPath(relativeUrl));
  }

  callAPIUpdatePlaygroundOnClick = (event) => {
    this.callAPIUpdatePlayground(event.target.href);
  }

  callAPIUpdatePlayground = (relativeUrl) => {
    //TODO: encode URI
    var reactParent = this;

    ajaxCall(
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
              <InputCurrentPath />
            </div>
          </div>
          <div className="row ui centered">
            <GetButton callAPIUpdatePlayground={this.callAPIUpdatePlayground} setErrorMessage={this.setErrorMessage}/>
            <ListButton setErrorMessage={this.setErrorMessage}/>
            <DCButton setErrorMessage={this.setErrorMessage}/>
            <DebugButton setErrorMessage={this.setErrorMessage}/>
            <RDFButton setErrorMessage={this.setErrorMessage}/>
            <EditButtons setErrorMessage={this.setErrorMessage}/>
            <DeleteButton setErrorMessage={this.setErrorMessage}/>
            <ModelButton setErrorMessage={this.setErrorMessage} setUrl={this.setUrl}/>
            <HistoricButton setErrorMessage={this.setErrorMessage}/>
          </div>

          {this.state.errorMessage ?
            <MessageErrorPath setParentState={this.setCurrentState} message={this.state.messageError} messageErrorDetails={this.state.messageErrorDetails}/>
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
