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

mookApp.filter("friendlyDate", function() {
    return function(input) {
        var inDate = new Date(input);
        inDate.setHours(0);
        inDate.setMinutes(0);
        inDate.setSeconds(0);
        inDate.setMilliseconds(0);

        var now = new Date();
        now.setHours(0);
        now.setMinutes(0);
        now.setSeconds(0);
        now.setMilliseconds(0);

        var elapsed = now.getTime() - inDate.getTime();
        var days = elapsed / (24 * 60 * 60 * 1000);

        if (days === 0) {
            return "I dag";
        } else if (days === 1) {
            return "I går";
        } else if (days === 2) {
            return "I forigårs";
        } else if (days < 7 && days > 2) {
            switch (inDate.getDay()) {
                case 0: return "Søndag";
                case 1: return "Mandag";
                case 2: return "Tirsdag";
                case 3: return "Onsdag";
                case 4: return "Torsdag";
                case 5: return "Fredag";
                case 6: return "Lørdag";
            }
        } else {
            // More than a week ago
            var options;
            if (inDate.getFullYear() === now.getFullYear()) {
                options = {day: "numeric", month: "long"};
            } else {
                options = {day: "numeric", month: "long", year: "numeric"};
            }
            return inDate.toLocaleDateString("nb", options);
        }
    }
});