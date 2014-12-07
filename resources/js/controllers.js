var mookApp = angular.module('mookApp', []);

mookApp.controller('EntryController', function ($scope, $http) {

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

    $http.get('entry').success(function(data) {
        $scope.entries = data;
    }).error(function(data, status) {
        handleError(status, data);
    });

    function handleError(status, data) {
        if (status == "401") {
        	console.log("Request not authenticated, redirecting to login page");
            window.location = "login.html";
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

    /*******************************************************************************************************************
     * Not possible yet to post JSON to Ceylon. Code from
     * http://victorblog.com/2012/12/20/make-angularjs-http-service-behave-like-jquery-ajax/
     ******************************************************************************************************************/

    // Use x-www-form-urlencoded Content-Type
    $http.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded;charset=utf-8';

    // Override $http service's default transformRequest
    $http.defaults.transformRequest = [function(data)
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
});