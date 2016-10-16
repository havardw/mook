
var mookControllers = angular.module("mookControllers", []);

mookControllers.controller("EntryController", function ($scope, $http, $location, $window, $interval, AuthService) {
    console.log("EntryController created");

    var sending = false;
    var loading = true;
    var autoSave;


    if (!AuthService.loggedIn) {
        console.log("Redirecting to login check");
        $location.path("/resume");
    } else {
        $http.get("api/entry", { headers: { auth: AuthService.token }}).success(function(data) {
            // Reverse sort for date
            data.sort(function(a, b) {
                if (a.date > b.date) {
                    return -1;
                } else if (b.date < a.date) {
                    return 1;
                } else {
                    return 0;
                }
            });
            $scope.entries = data;
            loading = false;
        }).error(function(data, status) {
            loading = false;
            handleError(status, data);
        });

    }

    function createAutoSave() {
        console.log("Checking autosave");
        if ($scope.entryForm.$dirty && !sending) {
            console.log("Saving entry");
            $window.localStorage.setItem("mook.entry.autosave", $scope.entry.text);
        }

    }

    // Defaults for new entry
    $scope.entry = newEntry();
    if ($window.localStorage.getItem("mook.entry.autosave")) {
        $scope.entry.text = $window.localStorage.getItem("mook.entry.autosave");
    }

    // Auto save
    autoSave = $interval(createAutoSave, 5000);
    $scope.$on('$destroy', function() {
        if (angular.isDefined(autoSave)) {
            $interval.cancel(autoSave);
            autoSave = undefined;
        }
    });

    $scope.update = function(entry) {
        sending = true;
        $http.post("api/entry", entry, { headers: { auth: AuthService.token }})
            .success(function() {
                $window.localStorage.removeItem("mook.entry.autosave");
                sending = false;
                entry.author = AuthService.name;
                $scope.entries.unshift(angular.copy(entry));
                $scope.entry = newEntry();
                $scope.entryForm.$setPristine();
            })
            .error(function(data, status) {
                sending = false;
                handleError(status, data);
            });
    };

    $scope.isEmpty = function(entry) {
        return entry.text === "";
    };

    $scope.isSending = function() {
        return sending;
    };

    $scope.isLoading = function() {
        return loading;
    }


    function handleError(status, data) {
        if (status == "401") {
        	console.log("Request not authenticated, redirecting to login page");
            AuthService.removeUserData();
            $location.path("/login");
        } else {
            var msg = "Server error " + status;
            if (data !== undefined && data !== "") {
                msg += "\n" + data;
            }
            alert(msg);
        }
    }

    function newEntry() {
        return {date: new Date(), text: "" };
    }
});

mookControllers.controller("LoginController", function ($scope, $http, $location, AuthService) {
    console.log("LoginController created");

    if (AuthService.loggedIn === true) {
        $location.path("/entries");
    }

    $scope.login = function (email, password, remember) {

        $http.post("api/login", {email: email, password: password})
            .success(function(data) {
                console.log("Login successful");
                console.log("Store login: " + remember);
                AuthService.setUserData(data.token, data.displayName, remember);
                $location.path("/entries");
            })
            .error(function(data, status) {
                if (status === 401) {
                    console.log("Login failed");
                    $scope.error = "Feil e-post eller passord";
                } else {
                    console.log(status + ": " + JSON.stringify(data));
                    $scope.error = "Ukjent feil, prøv igjen";
                }
            });
    }
});

mookControllers.controller("ResumeSessionController", function ($window, $http, $location, AuthService) {
    console.log("ResumeSessionController created");

    // Prefer sessionStorage, as it's likely to be more recent if it exists
    var authStr = $window.sessionStorage.getItem("auth");
    if (!authStr) {
        authStr = $window.localStorage.getItem("auth");
    }

    var auth;
    if (authStr) {
        console.log("Attempting to restore session");
        auth = JSON.parse(authStr);
        $http.post("api/resumeSession", {token: auth.token})
            .success(function(data) {
                console.log("Session restore successful");
                AuthService.setUserData(data.token, data.displayName, true);
                $location.path("/entries");
            })
            .error(function(data, status) {
                console.log("Session restore failed");
                $location.path("/login");
            });

    } else {
        console.log("No stored session, redirecting to login");
        $location.path("/login");
    }
});