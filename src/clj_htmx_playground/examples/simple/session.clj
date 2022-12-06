(ns clj-htmx-playground.examples.simple.session
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [ring.util.http-response :refer [ok]]))

(defn wrap-as-page [content]
  (html5
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    content))

(defn button [clicks]
  [:button#click-count.btn.btn-primary
   {:type    "submit"
    :hx-post "/simple/session/click"
    :hx-target "#click-count"
    :hx-swap "outerHTML"}
   (format "Clicked %s times!" clicks)])

(def page (wrap-as-page (button 0)))

(defn click [clicks]
  (html5 (button clicks)))

(def routes
  [["/simple"
    ["/session" {:handler (fn [_]
                            (-> (ok page)
                                (assoc-in [:session :clicks] 0)))}]
    ["/session/click" {:post
                       (fn [{:keys [session]}]
                         (let [{:keys [clicks]} session
                               clicks (inc (or clicks 0))]
                           (-> (ok (click clicks))
                               (assoc-in [:session :clicks] clicks))))}]]])