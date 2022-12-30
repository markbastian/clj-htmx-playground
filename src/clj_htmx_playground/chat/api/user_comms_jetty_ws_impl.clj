(ns clj-htmx-playground.chat.api.user-comms-jetty-ws-impl
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty9 :as jetty]
            [clj-htmx-playground.chat.api.user-api :as user-api]
            [clj-htmx-playground.chat.api.render-preference-api
             :as render-preference-api]))

(defrecord WebSocketUser [username ws render-preference])

(extend-type WebSocketUser
  user-api/IUser
  (send! [{:keys [username ws]} message]
    (log/infof "Sending to %s via ws." username)
    (jetty/send! ws message))
  render-preference-api/IRenderPreference
  (render-preference [{:keys [render-preference]}] render-preference))