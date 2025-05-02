import * as React from "react";
import axios, {AxiosError} from "axios";

import Login from "./Login";
import Entries from "./Entries";
import {parseQuery} from "./utils";
import {AuthenticationData } from "./domain";
import { SiteSelector } from "./SiteSelector";

const USER_DATA_KEY = "mook.userData";

export const OAUTH_STATE_KEY = "mook.oauthState";

interface ApplicationState {
    loginState: string;
    userData?: AuthenticationData;
    site?: string;
    globalError?: string;
    supportedBrowser: boolean;
    rememberOidcLogin: boolean;
    oidcAccessToken?: string;
    oidcError?: { code: string; email: string };
}

class MookApp extends React.Component<{}, ApplicationState> {

    constructor(props: {}) {
        super(props);

        // Check for supported browser
        let supportedBrowser = (!!(window.ProgressEvent)) && (!!(window.FormData))  // Checks for XHR 2
                               && (!!window.Promise); // Require native promise support

        let userDataStr = window.sessionStorage.getItem(USER_DATA_KEY);
        if (userDataStr === null) {
            userDataStr = window.localStorage.getItem(USER_DATA_KEY)
        }

        let loginState: string;
        let userData: AuthenticationData | undefined;
        let oidcAccessToken: string | undefined;
        let rememberOidcLogin = false;
        if (userDataStr !== null) {
            loginState = "restore";
            userData = JSON.parse(userDataStr);
        } else {
            let fragment = window.location.hash;
            if (fragment && fragment.length > 1) {
                let params = parseQuery(fragment.substring(1));

                if (params.hasOwnProperty("access_token") && params.hasOwnProperty("state")) {
                    // Remove fragment from URL
                    history.pushState(null, "", window.location.pathname + window.location.search);

                    let storedState = window.sessionStorage.getItem(OAUTH_STATE_KEY);
                    window.sessionStorage.removeItem(OAUTH_STATE_KEY);
                    if (storedState === params["state"]) {
                        loginState = "oidc";
                        oidcAccessToken = params["access_token"];

                        // IE11 doesn't support endsWith
                        let end = params["state"].substring(params["state"].length - 9);
                        rememberOidcLogin = end === "_remember";
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
            globalError: undefined,
            oidcError: undefined,
            supportedBrowser: supportedBrowser,
            oidcAccessToken: oidcAccessToken,
            rememberOidcLogin: rememberOidcLogin
        };
    }

    componentDidMount() {
        if (this.state.loginState === "restore") {
            axios.post("api/resumeSession", {token: this.state.userData?.token})
                .then(response => this.setState({userData: response.data, loginState: "loggedIn"}))
                .catch(error =>   {
                    if (error.response && error.response.status === 401) {
                        console.log("Stored session is not valid");
                        window.sessionStorage.removeItem(USER_DATA_KEY);
                        window.localStorage.removeItem(USER_DATA_KEY);
                        this.setState({globalError: undefined, loginState: "unauthorized"});
                    } else {
                        this.handleHttpError(error);
                    }
                });
        } else if (this.state.loginState === "oidc") {
            axios.post("api/oidc-login", {accessToken: this.state.oidcAccessToken})
                .then(response => {
                    this.setState({rememberOidcLogin: false, oidcAccessToken: undefined});
                    this.handleLogin(response.data, this.state.rememberOidcLogin);
                })
                .catch(error => {
                    if (error.response && error.response.status === 401) {
                        this.setState({globalError: undefined,
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

    handleLogin = (userData: AuthenticationData, rememberLogin: boolean): void => {
        if (rememberLogin) {
            window.localStorage.setItem(USER_DATA_KEY, JSON.stringify(userData));
        } else {
            window.sessionStorage.setItem(USER_DATA_KEY, JSON.stringify(userData));
        }

        this.setState({userData: userData, loginState: "loggedIn"});
        // Set site to active if we only have exactly one
        if (userData.sitePermissions.length === 1) {
            this.setState({site: userData.sitePermissions[0].path})
        }
    };

    handleHttpError = (error: AxiosError): void => {
        if (error.response === undefined) {
            this.setState({globalError: "Ingen nettverkskobling"});
        } else {
            if (error.response.status === 401) {
                this.setState({globalError: undefined, userData: undefined, loginState: "unauthorized"});
            } else {
                console.warn("Unhandled error: " + error.message);
                this.setState({globalError: "Ukjent feil"});
            }
        }
    };

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
        } else if (!this.state.site) {
            return (
                <SiteSelector
                    sites={this.state.userData!.sitePermissions}
                    onSelect={(site) => this.setState({site})}
                />
            );
        }
        else {
            return (<Entries site={this.state.site!} onHttpError={this.handleHttpError} userData={this.state.userData!} />);
        }

    }
}

export default MookApp;
