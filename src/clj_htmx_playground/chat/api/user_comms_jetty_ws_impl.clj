(ns clj-htmx-playground.chat.api.user-comms-jetty-ws-impl
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty9 :as jetty]
            [clj-htmx-playground.chat.api.user-api :as user-api]))

(defrecord WebSocketUser [username ws])

(extend-type WebSocketUser
  user-api/IUser
  (send! [{:keys [username ws]} message]
    (log/infof "Sending to %s via ws." username)
    (jetty/send! ws message)))