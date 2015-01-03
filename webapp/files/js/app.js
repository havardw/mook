var mookApp = angular.module('mookApp', ['ngRoute', 'mookControllers']);

mookApp.config(['$routeProvider',
    function ($routeProvider) {
        $routeProvider.
            when('/login', {
                templateUrl: 'partials/login.html',
                controller: 'LoginController'
            }).
            when('/entries', {
                templateUrl: 'partials/entries.html',
                controller: 'EntryController'
            }).
            otherwise({
                redirectTo: '/entries'
            });
    }]);

mookApp.factory('AuthService', ['$window', function ($window) {

    var auth = { loggedIn: false, token: undefined, name: undefined };
    var authStr = $window.sessionStorage.getItem("auth");


    if (authStr !== null) {
        auth = JSON.parse(authStr);
    }

    auth.setUserData = function (token, name) {
        auth.loggedIn = true;
        auth.token = token;
        auth.name = name;
        $window.sessionStorage.setItem("auth", JSON.stringify({loggedIn: true, token: token, name: name}));
    };

    auth.removeUserData = function () {
        auth.loggedIn = false;
        auth.token = undefined;
        auth.name = undefined;
        $window.sessionStorage.removeItem("auth");
    };

    return auth;
}]);