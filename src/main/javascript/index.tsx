import * as React from "react";
import MookApp from "./MookApp";
import {createRoot} from "react-dom/client";
import "./assets/mook.css";

window.addEventListener("DOMContentLoaded",
    function() {
        console.log("Loading app");

        let appElem = document.getElementById("applicationName");
        if (appElem != null) {
            appElem.innerText = "Mook";
        }
        document.title = "Mook";

        const root = createRoot(document.getElementById("root")!);
        root.render(<MookApp />);
    }, false);
