
function ImageViewController($http, $window, AuthService) {
    var ctrl = this;

    ctrl.url = "img/ajax-loader.gif";

    var config = {
        headers: {
            auth: AuthService.token
        },
        responseType: "blob"
    };


    var size;
    var maxDimension = Math.max($window.screen.width, $window.screen.height);
    if (maxDimension >= 600) {
        size = 800;
    } else if (maxDimension >= 400) {
        size = 600;
    } else {
        size = 400;
    }

    $http.get("api/image/resized/" + size + "/" + ctrl.image.name, config).success(function(data) {
        ctrl.url = $window.URL.createObjectURL(data);
    }).error(function(data, status) {
        console.log("Failed to load image data: " + status);
        ctrl.url = "";
    });

    ctrl.delete = function() {
        console.log("Delete: " + ctrl.image.name);
        ctrl.onDelete(ctrl.image);
    }
}

mookApp.component("mookimg", {
    templateUrl: "partials/image.html",
    controller: ImageViewController,
    bindings: {
        image: "=",
        editMode: "@",
        onDelete: "&"
    }
});