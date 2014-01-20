var mookApp = angular.module('mookApp', []);

mookApp.controller('EntryController', function ($scope, $http, $filter) {

    $scope.update = function(entry) {
        $http.post("/entry", entry).success(function() {
            $scope.entries.unshift(angular.copy(entry));
        });
    };

    $http.get('/entry').success(function(data) {
        $scope.entries = data;
    }).error(function(data, status, headers, config) {
        console.log("Error callback");
    });

    // Defaults for new entry
    $scope.entry = {date: $filter('date')(new Date(), "yyyy-MM-dd") };

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
                else if(value instanceof Object)
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