import * as React from "react";
import MookApp from "./MookApp";
import {createRoot} from "react-dom/client";

window.addEventListener("DOMContentLoaded",
    function() {
        console.log("Loading app");

        let appElem = document.getElementById("applicationName");
        if (appElem != null) {
            appElem.innerText = mookConfig.name;
        }
        document.title = mookConfig.name;

        const root = createRoot(document.getElementById("root")!);
        root.render(<MookApp />);
    }, false);
