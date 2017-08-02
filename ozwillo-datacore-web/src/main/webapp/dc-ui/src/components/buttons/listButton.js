import React from 'react';
import { connect } from 'react-redux';

import * as actions from '../../actions/actionIndex.js';
import {ajaxCall} from '../../utils.js';

class ListButton extends React.Component{
  listButton = () => {
    var relativeUrl = this.props.currentPath;
    ajaxCall(
      relativeUrl,
      (data) => {
        if (Object.prototype.toString.call( data ) === '[object Array]'){
          var list_data = [];
          for (var resource of data){
            list_data.push({"@id" : resource["@id"], "o:version": resource["o:version"]});
          }
          var resResourcesOrText = displayJsonListResult(list_data, relativeUrl);
        }
        else{
          data = {"@id" : data["@id"], "o:version": data["o:version"]};
          var resResourcesOrText = displayJsonObjectResult(data);
        }
        this.props.dispatch(actions.setCurrentDisplay(resResourcesOrText));
      },
      (xhr) => {
        this.props.setErrorMessage("Error accessing the current path", xhr.responseText);
      }
    );
  }

  render() {
    return (
      <button className="small ui button" onClick={this.listButton} id="listButton" data-content="List view (minimal)">l</button>
    );
  }
}

const mapStateToProps = (state) => ({
  currentPath: state.currentPath
})
export default ListButton = connect(mapStateToProps)(ListButton);
