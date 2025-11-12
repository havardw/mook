import {SitePermission} from "./domain";
import * as React from "react";
import {useLocation} from "wouter";

interface SiteSelectorProps {
    sites: SitePermission[];
}

export const SiteSelector: React.FC<SiteSelectorProps> = ({sites}) => {
    const [, navigate] = useLocation();

    const SelectorWrapper = {
        display: 'flex',
        flexDirection: 'column' as const,
        alignItems: 'center',
        padding: '2rem',
        gap: '1rem'
    };


    return (
        <div style={SelectorWrapper}>
            <h2>Velg side</h2>
            {sites.map(site => (
                <button
                    key={site.path}
                    onClick={() => navigate(`/site/${site.path}`)}
                >
                    {site.name}
                </button>
            ))}
        </div>
    );
};
