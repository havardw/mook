import React, { Component } from "react";
import axios from "axios";

class Login extends Component {

    constructor(props) {
        super(props);

        this.state = {email: "", password: "", remember: true, error: null};

        this.handleEmailChange = this.handleEmailChange.bind(this);
        this.handlePasswordChange = this.handlePasswordChange.bind(this);
        this.handleRememberChange = this.handleRememberChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleEmailChange(event) {
        this.setState({email: event.target.value});
    }

    handlePasswordChange(event) {
        this.setState({password: event.target.value});
    }

    handleRememberChange(event) {
        this.setState({remember: event.target.value});
    }


    handleSubmit(event) {
        event.preventDefault();
        axios.post("api/login", { email: this.state.email, password: this.state.password})
            .then(response => this.props.onLogin(response.data, this.state.remember))
            .catch(error => {
                console.warn("Login error: " + error.message);
                if (error.response === undefined) {
                    this.setState({error: "Ingen kontakt med server, sjekk at du er koblet på nett"});
                } else {
                    if (error.response.status === 401) {
                        this.setState({error: "Feil e-post eller passord"});
                    } else {
                        this.setState({error: "Ukjent feil (" + response.status + ")"});
                    }
                }
            });
    }

    render() {
        return (
            <div>
                <h1>Logg inn</h1>

                <form onSubmit={this.handleSubmit}>
                    <div className="grid">
                        <p><label htmlFor="email">E-post</label>
                            <input type="email" autoFocus="autofocus" value={this.state.email} onChange={this.handleEmailChange} /></p>
                        <p><label htmlFor="password">Passord</label>
                            <input type="password" value={this.state.password} onChange={this.handlePasswordChange}/></p>
                    </div>
                    <p><input type="checkbox" value={this.state.remember} onChange={this.handleRememberChange} /> <label htmlFor="remember">Husk innlogging</label></p>

                    <div className="error">{this.state.error}</div>

                    <p><input type="submit" value="Logg inn" /></p>
                </form>
            </div>
        )
    }
}

export default Login;
