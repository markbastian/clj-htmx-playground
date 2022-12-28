(ns clj-htmx-playground.ws-handlers
  (:require [ring.adapter.jetty9 :as jetty]))

(defn on-bytes [_context _ _ _ _]
  (println "on-bytes unhandled"))

(defn on-ping [_context ws payload] (println "PING")
  (jetty/send! ws payload))

(defn on-pong [_context _ _] (println "PONG"))