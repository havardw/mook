import * as React from "react";
import axios from "axios";
import {randomString} from "./utils";
import {OAUTH_STATE_KEY} from "./MookApp";
import {AuthenticationData} from "./domain";

interface LoginProps {
    onLogin(data: AuthenticationData, remember: boolean): void;
    oidcError?: {
        code: string;
        email: string;
    }
}

interface LoginState {
    email: string;
    password: string;
    remember: boolean;
    rememberOidc: boolean;
    passwordError?: string
}

class Login extends React.Component<LoginProps, LoginState> {

    constructor(props) {
        super(props);

        this.state = {email: "", password: "", remember: true, rememberOidc: false};
    }

    handleEmailChange = (event) => {
        this.setState({email: event.target.value});
    };

    handlePasswordChange = (event) => {
        this.setState({password: event.target.value});
    };

    handleRememberChange = (event) => {
        this.setState({remember: event.target.checked});
    };

    handleRememberOidcChange = (event) => {
        this.setState({rememberOidc: event.target.checked});
    };


    handleSubmit = (event) => {
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
                        this.setState({passwordError: "Ukjent feil (" + error.response.status + ")"});
                    }
                }
            });
    };

    oidcInit = () => {
        let random = randomString();

        if (this.state.rememberOidc) {
            random += "_remember";
        }

        window.sessionStorage.setItem(OAUTH_STATE_KEY, random);

        let authUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id="
            + mookConfig.googleId
            + "&response_type=token%20id_token"
            + "&scope=openid%20email"
            + "&redirect_uri=" + window.location
            + "&state=" + random
            + "&nonce=" + randomString(); // Not used by Mook

        console.log("Redirecting to OIDC URL " + authUrl);

        window.location.href = authUrl;
    };

    formatOidcError = () => {
        if (!this.props.oidcError) {
            return null;
        } else if  (this.props.oidcError.code === "oauth.not.registered") {
            return (<span>E-postadressen <strong>{this.props.oidcError.email}</strong> er ikke
                registrert. Sjekk at du har logget inn med riktig konto, eller ta kontakt med administrator.</span>);
        } else if (this.props.oidcError.code === "oauth.config.error") {
            return <span>Teknisk feil med innlogging. Ta kontakt med administrator.</span>;
        } else {
            return <span>Ukjent feil med innloging. Prøv igjen, og ta kontakt med administrator hvis det vedvarer.</span>;
        }
    };

    render() {
        let oidcError = this.formatOidcError();
        return (
            <div>
                <h1>Logg inn med Google</h1>
                <p>Logg inn med din Google-konto for Trondheim kommune, eller Gmail-konto.</p>
                <p>
                    <input type="checkbox" id="rememberOidc" checked={this.state.rememberOidc} onChange={this.handleRememberOidcChange} />
                    <label htmlFor="rememberOidc">Husk innlogging</label>
                </p>
                <p>
                    <button onClick={this.oidcInit}>Logg inn</button>
                </p>
                {oidcError && <p className="error">{oidcError}</p>}

                <h1>Brukernavn og passord</h1>
                <p>Logg inn med brukernavn og passord.</p>
                <form onSubmit={this.handleSubmit}>
                    <div className="grid">
                        <p><label htmlFor="email">E-post</label>
                            <input type="email" id="email" autoFocus={true} value={this.state.email} onChange={this.handleEmailChange} /></p>
                        <p><label htmlFor="password">Passord</label>
                            <input type="password" id="password" value={this.state.password} onChange={this.handlePasswordChange}/></p>
                    </div>
                    <p><input type="checkbox" id="remember" checked={this.state.remember} onChange={this.handleRememberChange} /> <label htmlFor="remember">Husk innlogging</label></p>

                    <div className="error">{this.state.passwordError}</div>

                    <p><input type="submit" value="Logg inn" /></p>
                </form>
            </div>
        )
    }
}

export default Login;
