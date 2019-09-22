import * as React from "react";
import * as ReactDOM from "react-dom";
import MookApp from "./MookApp";

window.addEventListener("DOMContentLoaded",
    function() {
        console.log("Loading app");
        ReactDOM.render(
            <MookApp />,
            document.getElementById("root")
        );
    }, false);
