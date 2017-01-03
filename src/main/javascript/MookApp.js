import React, { Component } from "react";
import axios from "axios";

import Login from "./Login";
import Entries from "./Entries";

class MookApp extends Component {

    constructor(props) {
        super(props);

        // Check for saved login
        let loginState = "unauthorized";
        let userData = window.sessionStorage.getItem("mook.userData");
        if (userData === null) {
            userData = window.localStorage.getItem("mook.userData");
        }
        if (userData !== null) {
            loginState = "restore";
            userData = JSON.parse(userData);
        }


        this.state = {
            userData: userData,
            loginState: loginState,
            globalError: null
        };



        this.handleLogin = this.handleLogin.bind(this);
        this.handleHttpError = this.handleHttpError.bind(this);
    }

    componentDidMount() {
        if (this.state.loginState === "restore") {
            axios.post("api/resumeSession", {token: this.state.userData.token})
                .then(response => this.setState({userData: response.data, loginState: "loggedIn"}))
                .catch(error => this.handleHttpError(error))
        }
    }

    handleLogin(userData, rememberLogin) {
        if (rememberLogin) {
            window.localStorage.setItem("mook.userData", JSON.stringify(userData));
        } else {
            window.sessionStorage.setItem("mook.userData", JSON.stringify(userData));
        }

        this.setState({userData: userData, loginState: "loggedIn"});
    }

    handleHttpError(error) {
        if (error.response === undefined) {
            this.setState({globalError: "Ingen nettverkskobling"});
        } else {
            if (error.response.status === 401) {
                this.setState({globalError: null, userData: null, loginState: "unauthorized"});
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
        } else if (this.state.loginState === "unauthorized") {
            return (<Login onLogin={this.handleLogin} />);
        } else if (this.state.loginState === "resume") {
            return (<em>Vent litt...</em>);
        }
        else {
            return (<Entries onHttpError={this.handleHttpError} userData={this.state.userData} />);
        }

    }
}

export default MookApp;
