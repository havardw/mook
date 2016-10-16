var mookApp = angular.module("mookApp", ["ngRoute", "mookControllers"]);

mookApp.config(["$routeProvider",
    function ($routeProvider) {
        $routeProvider.
            when("/login", {
                templateUrl: "partials/login.html",
                controller: "LoginController"
            }).
            when("/entries", {
                templateUrl: "partials/entries.html",
                controller: "EntryController"
            }).
            when("/resume", {
                templateUrl: "partials/wait.html",
                controller: "ResumeSessionController"
            }).
            otherwise({
                redirectTo: "/resume"
            });
    }]);

mookApp.factory("AuthService", ["$window", function ($window) {

    var auth = { loggedIn: false, token: undefined, name: undefined };
    var authStr = $window.sessionStorage.getItem("auth");
    var permanentStorage = false;

    function getStorage() {
        if (permanentStorage) {
            return $window.localStorage;
        } else {
            return $window.sessionStorage;
        }
    }


    if (authStr !== null) {
        auth = JSON.parse(authStr);
    }

    auth.setUserData = function (token, name, permanent) {
        auth.loggedIn = true;
        auth.token = token;
        auth.name = name;

        permanentStorage = permanent;

        getStorage().setItem("auth", JSON.stringify({loggedIn: true, token: token, name: name}));
    };

    auth.removeUserData = function () {
        auth.loggedIn = false;
        auth.token = undefined;
        auth.name = undefined;
        getStorage().removeItem("auth");
        permanentStorage = false;
    };

    return auth;
}]);