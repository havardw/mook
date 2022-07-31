import * as React from "react";
import MookApp from "./MookApp";
import {createRoot} from "react-dom/client";

window.addEventListener("DOMContentLoaded",
    function() {
        console.log("Loading app");

        document.getElementById("applicationName").innerText = mookConfig.name;
        document.title = mookConfig.name;

        const root = createRoot(document.getElementById("root"));
        root.render(<MookApp />);
    }, false);
