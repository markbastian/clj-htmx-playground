(ns clj-htmx-playground.chat.api.user
  (:require [clj-htmx-playground.chat.api.user-comms-jetty-ws-impl
             :as user-comms-jetty-ws-impl]))

(defn ws-user [username ws]
  (user-comms-jetty-ws-impl/->WebSocketUser username ws :htmx))
