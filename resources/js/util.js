// endsWith polyfill from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/endsWith
if (!String.prototype.endsWith) {
    Object.defineProperty(String.prototype, 'endsWith', {
        enumerable: false,
        configurable: false,
        writable: false,
        value: function (searchString, position) {
            position = position || this.length;
            position = position - searchString.length;
            var lastIndex = this.lastIndexOf(searchString);
            return lastIndex !== -1 && lastIndex === position;
        }
    });
}

// As a courtesy redirect if user is not logged in. Cookie is not available if coming from the login page
function verifyLogin() {
    console.log("Location " + window.location.href + ", referrer " + document.referrer);

    if (!document.cookie.contains("XSRF-TOKEN=") || !document.referrer.endsWith("/login.html")) {
        document.location = "login.html";
    }
}