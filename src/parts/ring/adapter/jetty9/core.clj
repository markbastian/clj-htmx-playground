(ns parts.ring.adapter.jetty9.core
  (:require [ring.adapter.jetty9 :as jetty9]
            [integrant.core :as ig]
            [clj-htmx-playground.middleware :refer [wrap-component]]
            [taoensso.timbre :as timbre])
  (:import [org.eclipse.jetty.server Server]))

(defmethod ig/init-key ::server [_ {:keys [handler] :as m}]
  (timbre/debug "Launching Jetty web server.")
  (jetty9/run-jetty (wrap-component handler m) m))

(defmethod ig/halt-key! ::server [_ ^Server server]
  (timbre/debug "Stopping Jetty web server.")
  (.stop server))