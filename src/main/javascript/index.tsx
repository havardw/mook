import * as React from "react";
import MookApp from "./MookApp";
import {createRoot} from "react-dom/client";
import "./assets/mook.css";

window.addEventListener("DOMContentLoaded",
    function() {
        console.log("Loading app");

        const root = createRoot(document.getElementById("root")!);
        root.render(<MookApp />);
    }, false);
