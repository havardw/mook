import React, { Component } from "react";

import Login from "./Login";
import Entries from "./Entries";

class MookApp extends Component {

    constructor(props) {
        super(props);
        this.state = {
            userData: null,
            globalError: null
        };

        this.handleLogin = this.handleLogin.bind(this);
        this.handleHttpError = this.handleHttpError.bind(this);
    }

    handleLogin(data) {
        this.setState({userData: data});
    }

    handleHttpError(error) {
        if (error.response === undefined) {
            this.setState({globalError: "Ingen nettverkskobling"});
        } else {
            if (error.response.status === 401) {
                this.setState({globalError: null, userData: null});
            } else {
                console.warn("Unhandled error: " + error.message);
                this.setState({globalError: "Ukjent feil"});
            }
        }
    }

    render() {
        if (this.state.globalError) {
            return (
                <div>
                    <h1>Beklager</h1>
                    <p>{this.state.globalError}</p>
                </div>
            );
        } else if (this.state.userData === null) {
            return (<Login onLogin={this.handleLogin} />);
        } else {
            return (<Entries onHttpError={this.handleHttpError} userData={this.state.userData} />);
        }

    }
}

export default MookApp;
