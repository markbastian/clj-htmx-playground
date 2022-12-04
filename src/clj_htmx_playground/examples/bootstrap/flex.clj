(ns clj-htmx-playground.examples.bootstrap.flex
  (:require [hiccup.page :refer [html5 include-css include-js]]))

(def flex
  (html5
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    [:div.d-flex.align-items-start.flex-column.bd-highlight.mb-3 {:style "height: 200px;"}
     [:div.mb-auto.p-2.bd-highlight "Flex item"]
     [:div.p-2.bd-highlight "Flex item"]
     [:div.p-2.bd-highlight "Flex item"]]
    [:div.d-flex.align-items-end.flex-column.bd-highlight.mb-3 {:style "height: 200px;"}
     [:div.p-2.bd-highlight "Flex item"]
     [:div.p-2.bd-highlight "Flex item"]
     [:div.mt-auto.p-2.bd-highlight "Flex item"]]))