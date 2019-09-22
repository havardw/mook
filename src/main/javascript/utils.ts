
export function randomString() {
    let array = new Uint32Array(10);
    window.crypto.getRandomValues(array);

    let str = "";
    array.forEach(function (uint) {
        str += uint.toString(36);
    });
    return str;
}

export function parseQuery(queryString) {
    let result = {};
    let pairs = queryString.split("&");
    pairs.forEach(function(pair) {
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