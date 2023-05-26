(ns clj-htmx-playground.web
  (:require
    [reitit.ring :as ring]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.util.http-response :refer [ok not-found]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.json :refer [wrap-json-response]]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.middleware.resource :refer [wrap-resource]]
    [ring.middleware.file :refer [wrap-file]]
    [clj-htmx-playground.examples.sidebar :as sidebar]
    [clj-htmx-playground.examples.pirates.routes :as pirates]
    [clj-htmx-playground.examples.bootstrap-sidebar :as bootstrap-sidebar]
    [clj-htmx-playground.examples.modal :as modal]
    [clj-htmx-playground.examples.offcanvas :as offcanvas]
    [clj-htmx-playground.examples.tailwind.simple :as tw.simple]
    [clj-htmx-playground.examples.bootstrap.flex :as bs.flex]
    [clj-htmx-playground.examples.simple.session :as ss]
    [clj-htmx-playground.chat.routes :as chat-routes]
    [clj-htmx-playground.decoder.routes :as decoder-routes]))

(def routes
  (reduce
    into
    [chat-routes/routes
     decoder-routes/routes
     ["/sb" {:handler (fn [_] (ok bootstrap-sidebar/sidebar))}]
     ["/offcanvas" {:handler (fn [_] (ok offcanvas/offcanvas))}]
     ["/tailwind"
      ["/simple" {:handler (fn [_] (ok tw.simple/page))}]]
     ["/bootstrap"
      ["/flex" {:handler (fn [_] (ok bs.flex/flex))}]]]
    [sidebar/routes
     modal/routes
     ss/routes
     pirates/routes]))

;; TODO - Add wrap-reload middleware
(def handler
  (wrap-reload
    (->
      (ring/ring-handler
        (ring/router
          routes
          {:data {:middleware [[wrap-defaults
                                (-> site-defaults
                                    (update :security dissoc :anti-forgery)
                                    (update :security dissoc :content-type-options)
                                    (update :responses dissoc :content-types))]
                               ;wrap-params
                               wrap-json-response
                               parameters/parameters-middleware
                               muuntaja/format-request-middleware
                               coercion/coerce-response-middleware
                               coercion/coerce-request-middleware]}})
        (constantly (not-found "Not found")))
      (wrap-resource "/clj_htmx_playground")
      (wrap-file "resources/clj_htmx_playground"))))