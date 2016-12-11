
function ImageViewController($http, $window, AuthService) {
    this.$onInit = function() {
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

        $http.get("api/image/resized/" + size + "/" + ctrl.image.name, config).then(function(response) {
            ctrl.url = $window.URL.createObjectURL(response.data);
        }).catch(function(response) {
            console.log("Failed to load image data: " + response.status);
            ctrl.url = "";
        });

        ctrl.delete = function() {
            console.log("Delete: " + ctrl.image.name);
            ctrl.onDelete(ctrl.image);
        }
    }
}

function ImageUploadController($http, $window, AuthService) {
    var ctrl = this;

    this.$onInit = function () {
        uploadImage();
    };

    function uploadImage() {
        console.log("uploadImage");
        ctrl.upload.url = $window.URL.createObjectURL(ctrl.upload.file);

        var config = {
            headers: {
                auth: AuthService.token,
                "Content-Type": "application/octet-stream"
            },
            transformRequest: angular.identity,
            uploadEventHandlers: {
                progress: function(event) {
                    var percent = (event.loaded / event.total) * 100;
                    ctrl.progressStyle = { width: percent + "%"}
                }
            }
        };

        $http.post("api/image", ctrl.upload.file, config)
            .then(function (response) {
                console.log("Image uploaded: " + JSON.stringify(response.data));
                ctrl.onComplete({upload: ctrl.upload, image: response.data});
            })
            .catch(function(response) {
                console.log("Upload error");
                console.log(response);
                var retry = $window.confirm("Kunne ikke legge til bilde.\nVil du prøve på nytt?");
                if (retry) {
                    uploadImage();
                } else {
                    ctrl.onFailed(ctrl.upload);
                }
            });
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

mookApp.component("mookupload", {
    templateUrl: "partials/imageupload.html",
    controller: ImageUploadController,
    bindings: {
        upload: "=",
        onComplete: "&",
        onFailed: "&"
    }
});