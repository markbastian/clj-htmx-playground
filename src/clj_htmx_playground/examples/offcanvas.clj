(ns clj-htmx-playground.examples.offcanvas
  (:require [hiccup.page :refer [html5 include-css include-js]]))

(def offcanvas
  "See https://getbootstrap.com/docs/5.2/components/offcanvas/"
  (html5
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    [:a.btn.btn-primary {:data-bs-toggle "offcanvas" :href "#offcanvasExample" :role "button" :aria-controls "offcanvasExample"} "Link with href"] [:button.btn.btn-primary {:type "button" :data-bs-toggle "offcanvas" :data-bs-target "#offcanvasExample" :aria-controls "offcanvasExample"} "Button with data-bs-target"]
    [:div#offcanvasExample.offcanvas.offcanvas-start {:tabindex "-1" :aria-labelledby "offcanvasExampleLabel"}
     [:div.offcanvas-header
      [:h5#offcanvasExampleLabel.offcanvas-title "Offcanvas"]
      [:button.btn-close {:type "button" :data-bs-dismiss "offcanvas" :aria-label "Close"}]]
     [:div.offcanvas-body
      [:div "Some text as placeholder. In real life you can have the elements you have chosen. Like, text, images, lists, etc."]
      [:div.dropdown.mt-3
       [:button.btn.btn-secondary.dropdown-toggle {:type "button" :data-bs-toggle "dropdown"} "Dropdown button"]
       [:ul.dropdown-menu
        [:li [:a.dropdown-item {:href "#"} "Action"]]
        [:li [:a.dropdown-item {:href "#"} "Another action"]]
        [:li [:a.dropdown-item {:href "#"} "Something else here"]]]]]]))

