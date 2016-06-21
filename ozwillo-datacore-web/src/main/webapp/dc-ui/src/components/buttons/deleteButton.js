import React from 'react';
import { connect } from 'react-redux';

import * as actions from '../../actions/actionIndex.js';
import {ajaxCall} from '../../utils.js';

class DeleteButton extends React.Component{
  deleteButton = () => {
    var relativeUrl = this.props.currentPath;

    //we first make a get request to get the current version of the object
    ajaxCall(
      relativeUrl,
      (data) => {
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
      (xhr) => {
        this.props.setErrorMessage("Can't delete this data", xhr.responseText);
      },
      null,
      'GET'
    );
  }

  deleteRessource = (relativeUrl, version) => {
    ajaxCall(
      relativeUrl,
      (data) => {
        this.props.dispatch(actions.setCurrentDisplay(data));
      },
      (xhr) => {
        this.props.setErrorMessage("Can't delete this data", xhr.responseText);
      },
      {"If-match": version},
      'DELETE'
    );
  }

  render() {
    return (
      <button className="small ui button" onClick={this.deleteButton} id="delButton" data-content="Delete data">del</button>
    );
  }
}

const mapStateToProps = (state) => ({
  currentPath: state.currentPath
})
export default DeleteButton = connect(mapStateToProps)(DeleteButton);
