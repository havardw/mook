
var mookControllers = angular.module('mookControllers', []);

mookControllers.controller('EntryController', function ($scope, $http, $location, AuthService) {
    console.log("EntryController created");

    console.log("Logged in: " + AuthService.loggedIn);
    if (!AuthService.loggedIn) {
        console.log("Redirecting to login");
        $location.path("/login");
    } else {
        $http.get(globalConfig.apiUrl + "/entry", { headers: { auth: AuthService.token }}).success(function(data) {
            console.log("Data for entries: " + JSON.stringify(data));
            $scope.entries = data;
        }).error(function(data, status) {
            handleError(status, data);
        });

    }

    var sending = false;

    $scope.update = function(entry) {
        sending = true;
        $http.post("entry", entry)
            .success(function() {
                sending = false;
                $scope.entries.push(angular.copy(entry));
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

    // Defaults for new entry
    $scope.entry = newEntry();

    setupPostAsParameters($http);
});

mookControllers.controller('LoginController', function ($scope, $http, $location, AuthService) {
    console.log("LoginController created");

    if (AuthService.loggedIn === true) {
        $location.path("/entries");
    }

    setupPostAsParameters($http);

    $scope.login = function (email, password) {

        $http.post(globalConfig.apiUrl + "/login", {email: email, password: password})
            .success(function(data) {
                console.log("Login successful");
                AuthService.setUserData(data.token, data.name);
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

function setupPostAsParameters(http) {
    /*******************************************************************************************************************
     * Not possible yet to post JSON to Ceylon. Code from
     * http://victorblog.com/2012/12/20/make-angularjs-http-service-behave-like-jquery-ajax/
     ******************************************************************************************************************/

    // Use x-www-form-urlencoded Content-Type
    http.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded;charset=utf-8';

    // Override $http service's default transformRequest
    http.defaults.transformRequest = [function(data)
    {
        /**
         * The workhorse; converts an object to x-www-form-urlencoded serialization.
         * @param {Object} obj
         * @return {String}
         */
        var param = function(obj)
        {
            var query = '';
            var name, value, fullSubName, subName, subValue, innerObj, i;

            for(name in obj)
            {
                value = obj[name];

                if(value instanceof Array)
                {
                    for(i=0; i<value.length; ++i)
                    {
                        subValue = value[i];
                        fullSubName = name + '[' + i + ']';
                        innerObj = {};
                        innerObj[fullSubName] = subValue;
                        query += param(innerObj) + '&';
                    }
                }
                else if (value instanceof Date) {
                    query += encodeURIComponent(name) + "=" + value.getTime() + "&";
                }
                else if (value instanceof Object)
                {
                    for(subName in value)
                    {
                        subValue = value[subName];
                        fullSubName = name + '[' + subName + ']';
                        innerObj = {};
                        innerObj[fullSubName] = subValue;
                        query += param(innerObj) + '&';
                    }
                }
                else if(value !== undefined && value !== null)
                {
                    query += encodeURIComponent(name) + '=' + encodeURIComponent(value) + '&';
                }
            }

            return query.length ? query.substr(0, query.length - 1) : query;
        };

        return angular.isObject(data) && String(data) !== '[object File]' ? param(data) : data;
    }];
}