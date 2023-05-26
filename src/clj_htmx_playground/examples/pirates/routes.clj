(ns clj-htmx-playground.examples.pirates.routes
  (:require [clj-htmx-playground.examples.pirates.pages :as pages]
            [clj-htmx-playground.utils :as u]
            [clj-htmx-playground.ws-handlers :as ws-handlers]
            [clojure.tools.logging :as log]
            [hiccup.page :refer [html5]]
            [hiccup.core :refer [html]]
            [ring.adapter.jetty9 :as jetty]
            [ring.util.http-response :refer [ok internal-server-error]]
            [sca.api.client :as client-api]
            [sca.api.lifecycle :as lifecycle-api]))

(defn on-connect [{:keys [clients path-params] :as context} ws]
  (let [{:keys [username]} path-params]
    (if (client-api/add-client! clients {:client-id username
                                         :transport :ws
                                         :ws        ws
                                         :transform :htmx})
      (do
        (println "XXXXXX")
        (let [command {:command  :change-room
                       :username username}]
          (lifecycle-api/sync-handler context command)))
      (jetty/close! ws))))

(defn on-text [{:keys [path-params] :as context} _ws text-message]
  (let [{:keys [username]} path-params
        json    (u/read-json text-message)
        command (keyword (get-in json [:HEADERS :HX-Trigger-Name]))]
    (lifecycle-api/sync-handler context (-> json
                                            (assoc
                                              :username username
                                              :command command)
                                            (dissoc :HEADERS)))))

(defn on-close [{:keys [clients path-params] :as context} _ws _status-code _reason]
  (let [{:keys [username]} path-params
        _       (log/debugf "on-close triggered for user: %s" username)
        _       (client-api/remove-client! clients username)
        command {:command :leave-chat :username username}]
    (lifecycle-api/sync-handler context command)))

(defn on-error [{:keys [clients path-params] :as context} _ws err]
  (let [{:keys [username]} path-params
        _       (log/debugf "on-error triggered for user: %s" username)
        _       (client-api/remove-client! clients username)
        command {:command :leave-chat :username username}]
    (lifecycle-api/sync-handler context command)
    (println err)))

(defn ws-upgrade-handler [context upgrade-request]
  (let [provided-subprotocols (:websocket-subprotocols upgrade-request)
        provided-extensions   (:websocket-extensions upgrade-request)]
    {:on-connect  (partial #'on-connect context)
     :on-text     (partial #'on-text context)
     :on-bytes    (partial #'ws-handlers/on-bytes context)
     :on-close    (partial #'on-close context)
     :on-ping     (partial #'ws-handlers/on-ping context)
     :on-pong     (partial #'ws-handlers/on-pong context)
     :on-error    (partial #'on-error context)
     :subprotocol (first provided-subprotocols)
     :extensions  provided-extensions}))

(defn ws-handler
  ([request]
   (if (jetty/ws-upgrade-request? request)
     (jetty/ws-upgrade-response (partial ws-upgrade-handler request))
     (internal-server-error "Cannot upgrade request")))
  ([request resp _raise]
   (resp (ws-handler request))))

(def routes
  [["/svg"
    ["/ws/:username" {:handler    ws-handler
                      :parameters {:path {:username string?}}}]
    ["/canvas" {:handler (fn [_]
                           (ok (pages/wrap-as-page (pages/main-page))))}]
    ["/canvas/click" {:post
                      (fn [{:keys [params] :as request}]
                        (let [{:keys [id]} params
                              [i j] (rand-nth pages/track)]
                          (ok
                            (html
                              (let [svg [:svg
                                         (pages/image
                                           id
                                           (:image params)
                                           {:x          (* i pages/tile-dim)
                                            :y          (* j pages/tile-dim)
                                            :transition "all ease-in 1s"})]]
                                (tap> svg)
                                svg)))))}]]])