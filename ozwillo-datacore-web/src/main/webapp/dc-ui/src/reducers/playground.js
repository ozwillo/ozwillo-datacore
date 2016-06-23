
const playground = (state, action) => {
    switch (action.type) {
        case 'SET_CURRENT_QUERY_PATH':
            return {
                ...state,
                currentPath: action.currentPath
              }
        case 'SET_CURRENT_DISPLAY_RDF':
        case 'SET_CURRENT_DISPLAY':
            return {
                ...state,
                currentJson: action.currentJson,
                codeView: action.codeView
            }
        case 'SET_EDITABLE':
            return {
                ...state,
                codeView: action.codeView,
                currentJson: action.currentJson
            }
        case 'SET_CURRENT_PROJECT':
          return {
              ...state,
              currentProject: action.currentProject,
          }
        default:
            return state
    }
}

export default playground
