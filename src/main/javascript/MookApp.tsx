import * as React from "react";
import axios, {AxiosError} from "axios";

import Login from "./Login";
import Entries from "./Entries";
import {parseQuery} from "./utils";
import {AuthenticationData, Site} from "./domain";
import { SiteSelector } from "./SiteSelector";
import { AppMenu } from "./AppMenu";
import {Route, Switch, useLocation, useRoute} from "wouter";
import {useEffect, useState} from "react";

const USER_DATA_KEY = "mook.userData";

export const OAUTH_STATE_KEY = "mook.oauthState";
export const AFTER_OAUTH_PATH = "mook.oauthTargetPath";

type LoginState = "unauthorized" | "oidc" | "loggedIn" | "resume";

interface OidcError {
    code: string;
    email: string;
}

// Check for supported browser
const supportedBrowser = (!!(window.ProgressEvent)) && (!!(window.FormData))  // Checks for XHR 2
    && (!!window.Promise); // Require native promise support

// Check for stored login or OIDC in progress when app loads
let initialState: LoginState = "unauthorized";
let resumeToken: string | undefined;
let oidcAccessToken: string | undefined;
let storeLogin = false;

let userDataStr = window.sessionStorage.getItem(USER_DATA_KEY);
if (userDataStr === null) {
    userDataStr = window.localStorage.getItem(USER_DATA_KEY);
    storeLogin = true;
}

if (userDataStr !== null) {
    const userData: AuthenticationData = JSON.parse(userDataStr);
    initialState = "resume"
    resumeToken = userData.token;
} else {
    let fragment = window.location.hash;
    if (fragment && fragment.length > 1) {
        let params = parseQuery(fragment.substring(1));

        if (params.hasOwnProperty("access_token") && params.hasOwnProperty("state")) {
            // Change path here to the called path, has the side benefit of removing OAuth data from URL
            const target = window.sessionStorage.getItem(AFTER_OAUTH_PATH);
            window.sessionStorage.removeItem(AFTER_OAUTH_PATH);
            history.pushState(null, "", target);

            let storedState = window.sessionStorage.getItem(OAUTH_STATE_KEY);
            window.sessionStorage.removeItem(OAUTH_STATE_KEY);
            if (storedState === params["state"]) {
                initialState = "oidc"
                oidcAccessToken = params["access_token"];
                storeLogin = params["state"].endsWith("_remember");
            } else {
                console.warn("OICD state mismatch. Stored: '" + storedState + "', received: '" + params["state"] + "'");
            }
        }
    }
}

function MookApp() {

    // State
    const [userData, setUserData] = useState<AuthenticationData | undefined>();
    const [loginState, setLoginState] = useState<LoginState>(initialState);
    const [site, setSite] = useState<Site | undefined>();
    const [globalError, setGlobalError] = useState<string | undefined>();
    const [oidcError, setOidcError] = useState<OidcError | undefined>();
    const [location, setLocation] = useLocation();
    const [siteMatch, siteParams] = useRoute("/site/:path");

    if (loginState === "resume") {
        axios.post("/api/resumeSession", {token: resumeToken})
            .then(response => {
                console.info("Session restored");
                setUserData(response.data);
                setLoginState("loggedIn");
                handleLogin(response.data, storeLogin);
            })
            .catch(error =>   {
                if (error.response && error.response.status === 401) {
                    console.info("Stored session is not valid");
                    window.sessionStorage.removeItem(USER_DATA_KEY);
                    window.localStorage.removeItem(USER_DATA_KEY);
                    setGlobalError(undefined);
                    setLoginState("unauthorized");
                } else {
                    handleHttpError(error);
                }
            });
    } else if (loginState === "oidc") {
        axios.post("/api/oidc-login", {accessToken: oidcAccessToken})
            .then(response => {
                handleLogin(response.data, storeLogin);
            })
            .catch(error => {
                if (error.response && error.response.status === 401) {
                    setGlobalError(undefined);
                    setLoginState("unauthorized");
                    setOidcError({
                        code: error.response.data.errorCode,
                        email: error.response.data.email
                    });
                } else {
                    handleHttpError(error);
                }
            })
    }


    const onSiteChange = (site: Site): void => {
        setGlobalError(undefined);
        setSite(site);
        setLocation("/site/" + site.path);
    };

    const handleLogin = (userData: AuthenticationData, rememberLogin: boolean): void => {
        if (rememberLogin) {
            window.localStorage.setItem(USER_DATA_KEY, JSON.stringify(userData));
        } else {
            window.sessionStorage.setItem(USER_DATA_KEY, JSON.stringify(userData));
        }
        setUserData(userData);
        setLoginState("loggedIn");

        // If direct access to a site, check if user has access
        if (siteMatch) {
            const matched = userData.sitePermissions.find(sp => sp.path === siteParams.path);
            if (!matched) {
                console.warn("Site '" + siteParams.path + "' not accessible for user");
                setGlobalError("Enten så finnes ikke siden, eller så har du ikke tilgang.");
                return;
            } else {
                setSite(matched);
            }
        }

        // Redirect to single site or selector
        if (location === "/") {
            if (userData!.sitePermissions.length === 1) {
                const selectedSite = userData.sitePermissions[0];
                setSite(selectedSite);
                setLocation("/site/" + selectedSite.path, { replace: true });
            } else {
                setLocation("/site", { replace: true });
            }
        }
    };

    const handleLogout = (): void => {
        // Reset state to unauthorized
        setUserData(undefined);
        setLoginState("unauthorized");
        setSite(undefined);
        setGlobalError(undefined);
        setLocation("/");
    };

    const handleHttpError = (error: AxiosError): void => {
        if (error.response === undefined) {
            setGlobalError("Ingen nettverkskobling");
        } else {
            if (error.response.status === 401) {
                setGlobalError(undefined);
                setUserData(undefined);
                setLoginState("unauthorized");
            } else {
                console.warn("Unhandled error: " + error.message);
                setGlobalError("Ukjent feil");
            }
        }
    };

    const renderContent = (): React.ReactNode => {
        if (!supportedBrowser) {
            return (
                <>
                    <h1>Uffda!</h1>
                    <p>Du bruker en for gammel nettleser, så Mook kommer ikke til å virke.</p>
                    <p>Bruk en annen nettleser, gjerne nyeste versjon
                        av <a href="https://www.mozilla.org/firefox/products/">Firefox</a> eller Google Chrome.</p>
                </>
            );
        } if (globalError) {
            return (
                <>
                    <h1>Beklager</h1>
                    <p>{globalError}</p>
                </>
            );
        } else if (loginState === "unauthorized") {
            return (<Login onLogin={handleLogin} oidcError={oidcError} />);
        } else if (loginState === "resume" || loginState === "oidc") {
            return (<em>Vent litt...</em>);
        } else {
            return (
                <Route>
                    <Switch>
                        <Route path="/site">
                            <SiteSelector
                                sites={userData!.sitePermissions}
                                onSiteChange={onSiteChange}
                            />
                        </Route>
                        <Route path="/site/:slug">
                            {params => <Entries key={params.slug} userData={userData!} site={params.slug} onHttpError={handleHttpError} />}
                        </Route>
                        <Route>
                            <>
                                <h1>Beklager</h1>
                                <p>Siden finnes ikke.</p>
                            </>
                        </Route>
                    </Switch>
                </Route>);
        }
    }

    useEffect(() => {
        document.title = site?.name ?? "Mook";
    }, [site])

    return (
        <>
            <header>
                <h1 >{site?.name ?? "Mook"}</h1>
                {userData && <AppMenu userData={userData} onSiteChange={onSiteChange} onLogout={handleLogout} />}
            </header>

            <main>
                {renderContent()}
            </main>
        </>
    )
}

export default MookApp;
