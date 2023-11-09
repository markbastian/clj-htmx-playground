(ns clj-htmx-playground.examples.table
  (:require
    [clojure.java.io :as io]
    [ring.util.http-response :refer [ok]]
    [hiccup.page :refer [html5 include-css include-js]]))

(def table
  #_[:table
     [:thead
      [:tr
       [:th "EMAIL"]]]
     [:tbody
      [:tr {:draggable   "true"
            :ondragstart "console.log('drag')"
            :ondragover  "console.log('over')"}
       [:td "jhon@gmail.com"]]
      [:tr {:draggable   "true"
            :ondragstart "console.log('drag')"
            :ondragover  "console.log('over')"}
       [:td "marygirl@yahoo.com"]]
      [:tr {:draggable   "true"
            :ondragstart "console.log('drag')"
            :ondragover  "console.log('over')"}
       [:td "cha24@yahoo.com"]]]]
  [:table.table.table-striped.table-dark
   [:thead
    [:tr
     [:th {:scope       "col"
           :draggable   "true"
           :ondragstart "start()"
           :ondragover  "dragover()"} "#"]
     [:th {:scope       "col"
           :draggable   "true"
           :ondragstart "start()"
           :ondragover  "dragover()"} "First"]
     [:th {:scope       "col"
           :draggable   "true"
           :ondragstart "start()"
           :ondragover  "dragover()"} "Last"]
     [:th {:scope       "col"
           :draggable   "true"
           :ondragstart "start()"
           :ondragover  "dragover()"} "Handle"]]]
   [:tbody
    [:tr {:draggable   "true"
          :ondragstart "start()"
          :ondragover  "dragover()"}
     [:th {:scope "row"} "1"]
     [:td "Mark"]
     [:td "Otto"]
     [:td "@mdo"]]
    [:tr {:draggable   "true"
          :ondragstart "start()"
          :ondragover  "dragover()"}
     [:th {:scope "row"} "2"]
     [:td "Jacob"]
     [:td "Thornton"]
     [:td "@fat"]]
    [:tr {:draggable   "true"
          :ondragstart "start()"
          :ondragover  "dragover()"}
     [:th {:scope "row"} "3"]
     [:td "Larry"]
     [:td "the Bird"]
     [:td "@twitter"]]]])

(defn landing-page [_]
  (ok
    (html5
      (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
      (include-js
        "https://unpkg.com/htmx.org@1.8.4"
        "https://unpkg.com/htmx.org/dist/ext/ws.js"
        "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"
        "clj_htmx_playground/playground.js")
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      table)))

(def routes
  [["/table" {:handler landing-page}]])


