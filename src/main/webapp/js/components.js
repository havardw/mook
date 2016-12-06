
function ImageViewController($http, $window, AuthService) {
    var ctrl = this;

    ctrl.url = "img/ajax-loader.gif";

    var config = {
        headers: {
            auth: AuthService.token
        },
        responseType: "blob"
    };

    $http.get("api/image/original/" + ctrl.image.name, config).success(function(data) {
        ctrl.url = $window.URL.createObjectURL(data);
    }).error(function(data, status) {
        console.log("Failed to load image data: " + status);
        ctrl.url = "";
    });
}

mookApp.component("mookimg", {
    templateUrl: "partials/image.html",
    controller: ImageViewController,
    bindings: {
        image: "=",
        editMode: "@"
    }
});