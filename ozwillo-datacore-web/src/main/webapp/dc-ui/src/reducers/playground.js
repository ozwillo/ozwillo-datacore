
const playground = (state, action) => {
    switch (action.type) {
        case 'SET_CURRENT_QUERY_PATH':
            return Object.assign({}, state, {
                currentPath: action.currentPath
              })
        case 'SET_CURRENT_DISPLAY':
            console.log(action);
            return Object.assign({}, state, {
                currentJson: action.currentJson
            })
        default:
            return state
    }
}

export default playground
