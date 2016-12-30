import React, { Component } from "react";

import Login from "./Login";
import Entries from "./Entries";

class MookApp extends Component {

    constructor(props) {
        super(props);
        this.state = {
            userData: null
        };

        this.handleLogin = this.handleLogin.bind(this);
    }

    handleLogin(data) {
        this.setState({userData: data});
    }

    render() {
        if (this.state.userData === null) {
            return (<Login onLogin={this.handleLogin} />);
        } else {
            return (<Entries />);
        }

    }
}

export default MookApp;
