(ns clj-htmx-playground.examples.sidebar
  (:require
    [clojure.java.io :as io]
    [ring.util.http-response :refer [ok]]
    [hiccup.page :refer [html5 include-css include-js]]))

(defn template [expanded?]
  [:div
   [:div#mySidebar.sidebar
    (if expanded?
      {:style "width: 250px;" :hx-swap-oob "true"}
      {:style "width: 0px;" :hx-swap-oob "true"})
    [:a.closebtn {:hx-post "/closeSidebar"} "x"]
    [:a {:href "#"} "About"]
    [:a {:href "#"} "Services"]
    [:a {:href "#"} "Clients"]
    [:a {:href "#"} "Contact"]]
   [:div#main
    (if expanded?
      {:style "margin-left: 250px;" :hx-swap-oob "true"}
      {:style "margin-left: 0px;" :hx-swap-oob "true"})
    [:h2 (if expanded?
           "Expanded Sidebar"
           "Collapsed Sidebar")]
    (when-not expanded? [:button.openbtn {:hx-post "/openSidebar"} "Open Sidebar"])
    [:p "Click on the hamburger menu/bar icon to open the sidebar, and push this content to the right."]]])

(defn landing-page [_]
  (ok
    (html5
      (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
      (include-js
        "https://unpkg.com/htmx.org@1.8.4"
        "https://unpkg.com/htmx.org/dist/ext/ws.js"
        "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:style (slurp (io/resource "clj_htmx_playground/styles.css"))]
      (template true))))

(def routes
  [["/sidebar" {:handler landing-page}]
   ["/openSidebar" {:post (fn [_] (ok (html5 (template true))))}]
   ["/closeSidebar" {:post (fn [_] (ok (html5 (template false))))}]])