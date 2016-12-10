
var mookControllers = angular.module("mookControllers", []);

mookControllers.controller("EntryController", function ($scope, $http, $location, $window, $interval, AuthService) {
    console.log("EntryController created");

    var sending = false;
    var loading = true;
    var imageStatus = [];
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
                    return 0;
                }
            });
            $scope.entries = response.data;
            loading = false;
        }).catch(function(response) {
            loading = false;
            handleError(response);
        });

    }

    function createAutoSave() {
        console.log("Checking autosave");
        if (!$scope.isEmpty() && !sending) {
            console.log("Saving entry");
            $window.localStorage.setItem("mook.entry.autosave", JSON.stringify($scope.entry));
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
                var file = input.files.item(i);
                var url = $window.URL.createObjectURL(file);
                var imageInfo = {url: url, loading: true};
                var index = imageStatus.length;
                imageStatus[length] = imageInfo;
                uploadImage(entry, file, imageInfo, index);
            }
        });

        input.click();
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
            if ($scope.isEmpty()) {
                $window.localStorage.removeItem("mook.entry.autosave");
            } else {
                $window.localStorage.setItem("mook.entry.autosave", JSON.stringify($scope.entry));
            }
        }
    };

    function uploadImage(entry, file, imageInfo, index) {
        var reader = new FileReader();
        reader.onload = function(e) {
            console.log("File read");

            var config = {
                headers: {
                    auth: AuthService.token,
                    "Content-Type": "application/octet-stream"
                },
                transformRequest: angular.identity
            };

            $http.post("api/image", e.target.result, config)
                .then(function (response) {
                    console.log("Image uploaded: " + JSON.stringify(response.data));
                    entry.images[index] = response.data;
                    imageInfo.loading = false;
                })
                .catch(handleError);
        };

        reader.readAsArrayBuffer(file);

    }


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
            .error(function(response) {
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