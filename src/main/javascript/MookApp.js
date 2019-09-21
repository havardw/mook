import React, { Component } from "react";
import axios from "axios";

import Login from "./Login";
import Entries from "./Entries";
import {parseQuery} from "./utils";

require('es6-promise/auto');

const USER_DATA_KEY = "mook." + mookConfig.prefix + ".userData";

export const OAUTH_STATE_KEY = "mook." + mookConfig.prefix + ".oauthState";

class MookApp extends Component {

    constructor(props) {
        super(props);

        // Check for supported browser
        let supportedBrowser = (!!(window.ProgressEvent)) && (!!(window.FormData)); // Checks for XHR 2


        // Check for saved login, legacy format first, convert to new format if found
        let userData = window.sessionStorage.getItem("mook.userData");
        if (userData !== null) {
            window.sessionStorage.removeItem("mook.userData");
            window.sessionStorage.setItem(USER_DATA_KEY, JSON.stringify(userData))
        } else {
            userData = window.localStorage.getItem("mook.userData");
            if (userData !== null) {
                window.localStorage.removeItem("mook.userData");
                window.localStorage.setItem(USER_DATA_KEY, JSON.stringify(userData))
            }
        }
        // Check for new format
        if (userData === null) {
            userData = window.sessionStorage.getItem(USER_DATA_KEY);
            if (userData === null) {
                userData = window.localStorage.getItem(USER_DATA_KEY)
            }
        }

        let loginState;
        let oidcAccessToken;
        let rememberOidcLogin = false;
        if (userData !== null) {
            loginState = "restore";
            userData = JSON.parse(userData);
        } else {
            let fragment = window.location.hash;
            if (fragment && fragment.length > 1) {
                let params = parseQuery(fragment.substring(1));

                if (params.hasOwnProperty("access_token") && params.hasOwnProperty("state")) {
                    // Remove fragment from URL
                    history.pushState(null, null, window.location.pathname + window.location.search);

                    let storedState = window.sessionStorage.getItem(OAUTH_STATE_KEY);
                    window.sessionStorage.removeItem(OAUTH_STATE_KEY);
                    if (storedState === params["state"]) {
                        loginState = "oidc";
                        oidcAccessToken = params["access_token"];
                        rememberOidcLogin = params["state"].endsWith("_remember");
                    } else {
                        console.log("OICD state mismatch. Stored: '" + storedState + "', received: '" + params["state"] + "'");
                        loginState = "unauthorized";
                    }
                } else {
                    loginState = "unauthorized";
                }
            } else {
                loginState = "unauthorized";
            }
        }


        this.state = {
            userData: userData,
            loginState: loginState,
            globalError: null,
            oidcError: null,
            supportedBrowser: supportedBrowser,
            oidcAccessToken: oidcAccessToken,
            rememberOidcLogin: rememberOidcLogin
        };



        this.handleLogin = this.handleLogin.bind(this);
        this.handleHttpError = this.handleHttpError.bind(this);
    }

    componentDidMount() {
        if (this.state.loginState === "restore") {
            axios.post("api/resumeSession", {token: this.state.userData.token})
                .then(response => this.setState({userData: response.data, loginState: "loggedIn"}))
                .catch(error => this.handleHttpError(error));
        } else if (this.state.loginState === "oidc") {
            axios.post("api/oidc-login", {accessToken: this.state.oidcAccessToken})
                .then(response => {
                    this.setState({rememberOidcLogin: undefined, oidcAccessToken: undefined});
                    this.handleLogin(response.data, this.state.rememberOidcLogin);
                })
                .catch(error => {
                    if (error.response && error.response.status === 401) {
                        this.setState({globalError: null,
                            loginState: "unauthorized",
                            oidcError: {
                                code: error.response.data.errorCode,
                                email: error.response.data.email
                            }
                        });
                    } else {
                        this.handleHttpError(error);
                    }
                })
        }
    }

    handleLogin(userData, rememberLogin) {
        if (rememberLogin) {
            window.localStorage.setItem(USER_DATA_KEY, JSON.stringify(userData));
        } else {
            window.sessionStorage.setItem(USER_DATA_KEY, JSON.stringify(userData));
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
        if (!this.state.supportedBrowser) {
            return (
                <div>
                    <h1>Uffda!</h1>
                    <p>Du bruker en for gammel nettleser, så Mook kommer ikke til å virke.</p>
                    <p>Bruk en annen nettleser, gjerne nyeste versjon
                        av <a href="https://www.mozilla.org/firefox/products/">Firefox</a> eller Google Chrome.</p>
                </div>
            );
        } if (this.state.globalError) {
            return (
                <div>
                    <h1>Beklager</h1>
                    <p>{this.state.globalError}</p>
                </div>
            );
        } else if (this.state.loginState === "unauthorized") {
            return (<Login onLogin={this.handleLogin} oidcError={this.state.oidcError} />);
        } else if (this.state.loginState === "resume" || this.state.loginState === "oidc") {
            return (<em>Vent litt...</em>);
        }
        else {
            return (<Entries onHttpError={this.handleHttpError} userData={this.state.userData} />);
        }

    }
}

export default MookApp;
