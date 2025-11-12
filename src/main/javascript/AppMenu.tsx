import * as React from "react";
import axios from "axios";
import { AuthenticationData } from "./domain";
import {Link} from "wouter";

interface AppMenuProps {
    userData: AuthenticationData;
    currentSite?: string;
    onSiteChange: (site: string, siteName: string) => void;
    onLogout: () => void;
}

interface AppMenuState {
    isOpen: boolean;
}

export class AppMenu extends React.Component<AppMenuProps, AppMenuState> {
    private menuRef = React.createRef<HTMLDivElement>();

    constructor(props: AppMenuProps) {
        super(props);
        this.state = {
            isOpen: false
        };
    }

    componentDidMount() {
        // Add event listener to close menu when clicking outside
        document.addEventListener("mousedown", this.handleClickOutside);
    }

    componentWillUnmount() {
        // Remove event listener
        document.removeEventListener("mousedown", this.handleClickOutside);
    }

    handleClickOutside = (event: MouseEvent) => {
        if (this.menuRef.current && !this.menuRef.current.contains(event.target as Node)) {
            this.setState({ isOpen: false });
        }
    };

    toggleMenu = () => {
        this.setState(prevState => ({ isOpen: !prevState.isOpen }));
    };

    handleLogout = () => {
        // Call the logout API
        axios.post("/api/logout", { token: this.props.userData.token })
            .then(() => {
                // Clear session storage and local storage
                window.sessionStorage.removeItem("mook.userData");
                window.localStorage.removeItem("mook.userData");

                // Call the onLogout callback
                this.props.onLogout();
            })
            .catch(error => {
                console.error("Logout failed", error);
                // Still try to logout locally even if the API call fails
                window.sessionStorage.removeItem("mook.userData");
                window.localStorage.removeItem("mook.userData");
                this.props.onLogout();
            });
    };

    render() {
        const { userData, currentSite } = this.props;
        const { isOpen } = this.state;

        const menuStyles: React.CSSProperties = {
            position: 'relative',
            display: 'inline-block',
            marginLeft: 'auto',
            marginRight: '20px'
        };

        const buttonStyles: React.CSSProperties = {
            background: 'none',
            border: '1px solid rgba(255, 255, 255, 0.5)',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '24px',
            display: 'flex',
            alignItems: 'center',
            color: 'white',
            padding: '2px 6px'
        };

        const dropdownStyles: React.CSSProperties = {
            display: isOpen ? 'block' : 'none',
            position: 'absolute',
            right: 0,
            backgroundColor: '#f9f9f9',
            minWidth: '160px',
            boxShadow: '0px 8px 16px 0px rgba(0,0,0,0.2)',
            zIndex: 1,
            borderRadius: '4px'
        };

        const dropdownItemStyles: React.CSSProperties = {
            color: 'black',
            padding: '12px 16px',
            textDecoration: 'none',
            display: 'block',
            cursor: 'pointer',
            textAlign: 'left'
        };

        const siteItemStyles: React.CSSProperties = {
            ...dropdownItemStyles,
            backgroundColor: 'transparent'
        };

        const currentSiteItemStyles: React.CSSProperties = {
            ...siteItemStyles,
            fontWeight: 'bold',
            backgroundColor: '#e9e9e9'
        };

        const dividerStyles: React.CSSProperties = {
            height: '1px',
            backgroundColor: '#ddd',
            margin: '8px 0'
        };

        // Only show site switching if user has access to more than one site
        const showSiteSwitcher = userData.sitePermissions.length > 1;

        return (
            <div style={menuStyles} ref={this.menuRef}>
                <button style={buttonStyles} onClick={this.toggleMenu}>
                    ≡
                </button>
                <div style={dropdownStyles}>
                    {showSiteSwitcher && (
                        <>
                            <div style={{ padding: '12px 16px', fontWeight: 'bold', color: 'black' }}>Bytt side:</div>
                            {userData.sitePermissions.map(site => (
                                <Link
                                    key={site.path}
                                    style={site.path === currentSite ? currentSiteItemStyles : siteItemStyles}
                                    href={"/site/" + site.path}
                                    onClick={() => this.setState({isOpen: false})}
                                >
                                    {site.name}
                                </Link>
                            ))}
                            <div style={dividerStyles}></div>
                        </>
                    )}
                    <div style={dropdownItemStyles} onClick={this.handleLogout}>
                        Logg ut
                    </div>
                </div>
            </div>
        );
    }
}
