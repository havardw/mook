
export interface AuthenticationData {
    id: number;
    email: string;
    displayName: string;
    token: string;
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