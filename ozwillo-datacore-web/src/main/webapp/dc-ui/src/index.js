import React from "react"
import ReactDOM from "react-dom";
import { Provider } from 'react-redux';
import { createStore } from 'redux';
import { Router, Route, Link, browserHistory} from 'react-router';

import App from "./components/app.js";
import playground  from "./reducers/playground.js";

import UserManual from "./components/reading/userManual.js";
import ExploreModelsProjects from "./components/reading/exploreModelsProjects.js";
import faqs from "./components/reading/faqs.js";
import commonQueries from "./components/reading/commonQueries.js";
import apiDetails from "./components/reading/apiDetails.js";
import serverConfiguration from "./components/reading/serverConfiguration.js";

const initialState = {
  readings: [
    {"title":"Playground User Manual", "path": "user-manual", "component": UserManual},
    {"title":"Explore models and projects", "path": "explore-model", "component": ExploreModelsProjects},
    {"title":"Common queries", "path": "common-queries", "component": commonQueries},
    {"title":"API details", "path": "API-details", "component": apiDetails},
    {"title":"FAQS", "path": "FAQS", "component": faqs},
    {"title":"Server configuration", "path": "server-configuration", "component": serverConfiguration},
  ],
  currentPath: "/dc/type/dcmo:model_0",
};

let store = createStore(playground, initialState);


ReactDOM.render(
  <Provider store={store}>
    <Router history={browserHistory}>
      <Route path="/dc-ui/index.html" component={App}>
        {initialState.readings.map(function(reading, i){
          return <Route path={reading.path} component={reading.component} key={i}/>;
        }, this)}
      </Route>
    </Router>
  </Provider>,
  document.getElementsByClassName('generalContainer')[0]
);
