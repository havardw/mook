import React, { Component } from "react";
import axios from "axios";
import { GoogleLogin } from 'react-google-login';

class Login extends Component {

    constructor(props) {
        super(props);

        this.state = {email: "", password: "", remember: true, passwordError: null, oauthError: null};

        this.handleEmailChange = this.handleEmailChange.bind(this);
        this.handlePasswordChange = this.handlePasswordChange.bind(this);
        this.handleRememberChange = this.handleRememberChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.verifyGoogleLogin = this.verifyGoogleLogin.bind(this);
        this.handleGoogleLoginError = this.handleGoogleLoginError.bind(this);
    }

    handleEmailChange(event) {
        this.setState({email: event.target.value});
    }

    handlePasswordChange(event) {
        this.setState({password: event.target.value});
    }

    handleRememberChange(event) {
        this.setState({remember: event.target.checked});
    }


    handleSubmit(event) {
        event.preventDefault();
        axios.post("api/login", { email: this.state.email, password: this.state.password})
            .then(response => this.props.onLogin(response.data, this.state.remember))
            .catch(error => {
                console.warn("Password login error: " + error.message);
                if (error.response === undefined) {
                    this.setState({passwordError: "Ingen kontakt med server, sjekk at du er koblet på nett"});
                } else {
                    if (error.response.status === 401) {
                        if (error.response.data.errorCode === "password.mismatch") {
                            this.setState({passwordError: "Feil e-post eller passord"});
                        } else {
                            this.setState({passwordError: "Ukjent feil"})
                        }
                    } else {
                        this.setState({passwordError: "Ukjent feil (" + response.status + ")"});
                    }
                }
            });
    }

    verifyGoogleLogin(response) {
        axios.post("api/google-id", {tokenId: response.tokenId})
            .then(response => this.props.onLogin(response.data, this.state.remember))
            .catch(error => {
                if (error.response.status === 401) {
                    let reason = error.response.data.errorCode;
                    if (reason === "oauth.not.registered") {
                        this.setState({oauthError: "E-postadressen din er ikke registrert. Ta kontakt med administrator."})
                    } else if (reason === "oauth.config.error") {
                        this.setState({oauthError: "Teknisk feil med innlogging. Ta kontakt med administrator."})
                    } else {
                        this.setState({oauthError: "Ukjent feil med innloging. Prøv igjen, og ta kontakt med administrator hvis det vedvarer."})
                    }
                } else {
                    console.warn("Google login error: " + error.message);
                    this.setState({oauthError: "Ukjent feil."});
                }
            });
    }

    handleGoogleLoginError(response) {
        console.log("Google login error", response);
        if (response.error === "popup_closed_by_user" || response.error === "access_denied") {
            this.setState({oauthError: "Innlogging ble avbrutt."});
        } else if (response.error === "idpiframe_initialization_failed") {
            this.setState({oauthError: "Tekniske problemer med Google-innlogging, ta kontakt med administrator."})
        } else {
            this.setState({oauthError: "Ukjent feil."});
        }
    }

    render() {
        return (
            <div>
                <h1>Brukernavn og passord</h1>
                <form onSubmit={this.handleSubmit}>
                    <div className="grid">
                        <p><label htmlFor="email">E-post</label>
                            <input type="email" id="email" autoFocus="autofocus" value={this.state.email} onChange={this.handleEmailChange} /></p>
                        <p><label htmlFor="password">Passord</label>
                            <input type="password" id="password" value={this.state.password} onChange={this.handlePasswordChange}/></p>
                    </div>
                    <p><input type="checkbox" id="remember" checked={this.state.remember} onChange={this.handleRememberChange} /> <label htmlFor="remember">Husk innlogging</label></p>

                    <div className="error">{this.state.passwordError}</div>

                    <p><input type="submit" value="Logg inn" /></p>
                </form>

                <h1>Google-konto</h1>
                <p>
                    <GoogleLogin
                        clientId={mookConfig.googleId}
                        buttonText="Logg inn med Google-konto"
                        onSuccess={this.verifyGoogleLogin}
                        onFailure={this.handleGoogleLoginError}
                    />
                </p>
                <p className="error">{this.state.oauthError}</p>
            </div>
        )
    }
}

export default Login;
