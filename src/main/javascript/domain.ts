
export type Permission = 'ADMIN' | 'EDIT';

export interface Site {
    name: string;
    path: string;
}

export interface SitePermission extends Site{
    permission: Permission;
}

export interface AuthenticationData {
    id: number;
    email: string;
    displayName: string;
    token: string;
    sitePermissions: SitePermission[];
}

export interface Entry {
    id?: number;
    author?: string;
    text: string;
    date: string;
    images: Image[];
}

export interface Image {
    id: number;
    name: string;
    caption: string;
}
