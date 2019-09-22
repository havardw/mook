
export function randomString(): string {
    let crypto = window.crypto;
    if (!crypto) {
        // Needed for IE11, IE10 and earlier are not supported
        crypto = (window as any).msCrypto;
    }

    let array = new Uint32Array(10);
    crypto.getRandomValues(array);

    let str = "";
    for (let i = 0; i < array.length; i++) { // Uint32Array doesn't support forEach in IE11
        str += array[i].toString(36);
    }

    return str;
}

export function parseQuery(queryString: string): {[key: string]: string} {
    let result: {[key: string]: string} = {};
    let pairs = queryString.split("&");
    pairs.forEach(pair => {
        let firstEquals = pair.indexOf("=");
        if (firstEquals === -1) {
            result[decodeURIComponent(pair)] = undefined;
        } else if (firstEquals === pair.length - 1) {
            result[decodeURIComponent(pair.substring(0, firstEquals))] = undefined;
        } else {
            result[decodeURIComponent(pair.substring(0, firstEquals))] = decodeURIComponent(pair.substring(firstEquals + 1));
        }
    });

    return result;
}