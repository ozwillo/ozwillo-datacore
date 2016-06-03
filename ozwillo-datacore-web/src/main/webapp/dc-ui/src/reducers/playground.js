
const playground = (state, action) => {
    switch (action.type) {
        case 'SET_CURRENT_QUERY_PATH':
            console.log(action.currentPath);
            return Object.assign({}, state, {
                currentPath: action.currentPath
              })
        default:
            return state
    }
}

export default playground
