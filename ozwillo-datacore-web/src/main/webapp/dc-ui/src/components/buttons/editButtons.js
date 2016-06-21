import React from 'react';
import { connect } from 'react-redux';
import ReactCSSTransitionGroup from 'react-addons-css-transition-group';

import * as actions from '../../actions/actionIndex.js';
import {ajaxCall} from '../../utils.js';

class EditButtons extends React.Component{
  editButton = () => {
    var relativeUrl = this.props.currentPath;
    //we make a call in order to get the datas without the html formating
    ajaxCall(
      relativeUrl,
      (data) => {
        this.props.dispatch(actions.setCurrentQueryPath(relativeUrl));
        this.props.dispatch(actions.setEditable(data));
      },
      (xhr) => {
        this.props.setErrorMessage("", xhr.responseText);
      },
      null,
      'GET'
    );
  }

  putButton = () => {
    this.putOrPostButton("PUT");
  }

  postButton = () => {
    this.putOrPostButton("POST");
  }

  putOrPostButton = (requestType) => {
    var relativeUrl = this.props.currentPath;
    ajaxCall(
      "/dc",
      (data) => {
        if (Object.prototype.toString.call(data) === '[object Array]' ){
          var resource = displayJsonListResult(data, relativeUrl);
        }
        else{
          var resource = displayJsonObjectResult(data);
        }
        this.props.dispatch(actions.setCurrentQueryPath(relativeUrl));
        this.props.dispatch(actions.setCurrentDisplay(resource));
      },
      (xhr) => {
        this.props.setErrorMessage("Impossible to Post or Put datas", xhr.responseText);
      },
      {"Content-Type": "application/json"},
      requestType,
      this.props.currentJson
    );
  }

  render() {
    if(this.props.codeView === "editable"){
      var itemsPostPut = <div>
            <button className="small ui button" id="postButton" onClick={this.postButton}>POST</button>
            <button className="small ui button" id="putButton" onClick={this.putButton}>PUT</button>
          </div>;
    }
    else{
      var itemEdit = <button className="small ui button" id="editButton" onClick={this.editButton}>edit</button>;
    }
    return (
      <div>
          {itemEdit}
          {itemsPostPut}
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  currentJson: state.currentJson,
  currentPath: state.currentPath,
  codeView: state.codeView
})
export default EditButtons = connect(mapStateToProps)(EditButtons);
