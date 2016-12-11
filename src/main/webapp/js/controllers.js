
var mookControllers = angular.module("mookControllers", []);

mookControllers.controller("EntryController", function ($scope, $http, $location, $window, $interval, AuthService) {
    console.log("EntryController created");

    var sending = false;
    var loading = true;
    $scope.uploads = [];
    var autoSave;


    if (!AuthService.loggedIn) {
        console.log("Redirecting to login check");
        $location.path("/resume");
    } else {
        $http.get("api/entry", { headers: { auth: AuthService.token }})
            .then(function(response) {
            // Reverse sort for date
            response.data.sort(function(a, b) {
                if (a.date > b.date) {
                    return -1;
                } else if (b.date > a.date) {
                    return 1;
                } else {
                    // Secondary sort by ID
                    return b.id - a.id;
                }
            });
            $scope.entries = response.data;
            loading = false;
        }).catch(function(response) {
            loading = false;
            handleError(response);
        });

    }

    function updateAutoSave() {
        if (!$scope.isEmpty() && !sending) {
            $window.localStorage.setItem("mook.entry.autosave", JSON.stringify($scope.entry));
        } else if ($scope.isEmpty() && $window.localStorage.getItem("mook.entry.autosave")) {
            $window.localStorage.removeItem("mook.entry.autosave");
        }
    }

    // Defaults for new entry
    $scope.entry = newEntry();
    if ($window.localStorage.getItem("mook.entry.autosave")) {
        var restoredEntry = JSON.parse($window.localStorage.getItem("mook.entry.autosave"));
        // Date restored as String, convert to date again
        restoredEntry.date = new Date(restoredEntry.date);
        $scope.entry = restoredEntry;
    }

    // Auto save
    autoSave = $interval(updateAutoSave, 5000);
    $scope.$on('$destroy', function() {
        if (angular.isDefined(autoSave)) {
            $interval.cancel(autoSave);
            updateAutoSave();
            autoSave = undefined;
        }
    });

    $scope.update = function(entry) {
        sending = true;

        $http.post("api/entry", entry, { headers: { auth: AuthService.token }})
            .then(function() {
                $window.localStorage.removeItem("mook.entry.autosave");
                sending = false;
                entry.author = AuthService.name;
                $scope.entries.unshift(angular.copy(entry));
                $scope.entry = newEntry();
                $scope.entryForm.$setPristine();
            })
            .catch(function(response) {
                sending = false;
                handleError(response);
            });
    };

    $scope.isEmpty = function() {
        return $scope.entry.text === "" && $scope.entry.images.length === 0;
    };

    $scope.isSending = function() {
        return sending;
    };

    $scope.isLoading = function() {
        return loading;
    };

    $scope.addImage = function(entry) {
        var input = document.getElementById("imageUpload");

        // Fixme Don't add an event listener for each click, but it works for now to get access to entry
        input.addEventListener("change", function() {
            for (var i = 0; i < input.files.length; i++) {
                var upload = {
                    url: $window.URL.createObjectURL(input.files.item(i)),
                    file: input.files.item(i)
                };
                $scope.uploads.push(upload);
            }
        });

        input.click();
    };

    $scope.uploadComplete = function(upload, image) {
        console.log("Upload complete");
        console.log(upload);
        console.log(image);
        $scope.removeUpload(upload);
        if (image) {
            $scope.entry.images.push(image);
        } else {
            console.log("Image not set");
        }

    };

    $scope.removeUpload = function(upload) {
        var index = $scope.uploads.indexOf(upload);
        if (index != -1) {
            $scope.uploads.splice(index, 1);
        }
    };

    $scope.deleteImage = function(image) {
        console.log("Entry.deleteImage: " + image.name);

        var index = $scope.entry.images.indexOf(image);
        if (index >= 0) {
            $scope.entry.images.splice(index, 1);
            $http.delete("api/image/original/" + image.name, { headers: { auth: AuthService.token }})
                .then(function() {
                    console.log("Deleted image " + image.name);
                })
                .catch(function(response) {
                    console.log("Failed to delete image " + image.name +"(" + response.status + "): " + response.data);
                });

            // Make sure to clear autosave if the entry is empty
            updateAutoSave();
        }
    };

    function handleError(response) {
        if (response.status == "401") {
        	console.log("Request not authenticated, redirecting to login page");
            AuthService.removeUserData();
            $location.path("/login");
        } else {
            var msg = "Server error " + response.status;
            if (data !== undefined && data !== "") {
                msg += "\n" + response.data;
            }
            alert(msg);
        }
    }

    function newEntry() {
        return {date: new Date(), text: "", images: [] };
    }
});

mookControllers.controller("LoginController", function ($scope, $http, $location, AuthService) {
    console.log("LoginController created");

    if (AuthService.loggedIn === true) {
        $location.path("/entries");
    }

    $scope.login = function (email, password, remember) {

        $http.post("api/login", {email: email, password: password})
            .then(function(response) {
                console.log("Login successful");
                console.log("Store login: " + remember);
                AuthService.setUserData(response.data.token, response.data.displayName, remember);
                $location.path("/entries");
            })
            .catch(function(response) {
                if (response.status === 401) {
                    console.log("Login failed");
                    $scope.error = "Feil e-post eller passord";
                } else {
                    console.log(response.status + ": " + JSON.stringify(response.data));
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
            .then(function(response) {
                console.log("Session restore successful");
                AuthService.setUserData(response.data.token, response.data.displayName, true);
                $location.path("/entries");
            })
            .catch(function() {
                console.log("Session restore failed");
                $location.path("/login");
            });

    } else {
        console.log("No stored session, redirecting to login");
        $location.path("/login");
    }
});