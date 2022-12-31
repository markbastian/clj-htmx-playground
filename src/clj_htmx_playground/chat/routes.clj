(ns clj-htmx-playground.chat.routes
  (:require
    [clj-htmx-playground.chat.commands :as commands]
    [clj-htmx-playground.chat.pages :as chat-pages]
    [clj-htmx-playground.utils :as u]
    [clojure.tools.logging :as log]
    [ring.adapter.jetty9 :as jetty]
    [ring.util.http-response :refer [ok internal-server-error]]
    [clj-htmx-playground.ws-handlers :as ws-handlers]
    [hiccup.page :refer [html5]]))

(defn add-user! [state {:keys [username] :as m}]
  (if-not (@state username)
    (do
      (log/debugf "Adding user: %s" username)
      (swap! state assoc username m))
    (log/debugf "User '%s' already exists. Not adding." username)))

(defn remove-user! [state username]
  (when-some [{:keys [username]} (@state username)]
    (log/debugf "Removing user: %s" username)
    (swap! state dissoc username)))

(defn on-connect [{:keys [users path-params] :as context} ws]
  (let [{:keys [username room-name]} path-params]
    (if (add-user! users {:username  username
                          :transport :ws
                          :ws        ws})
      (let [command {:command   :change-room
                     :username  username
                     :room-name room-name}]
        (commands/handle-command context command))
      (do
        (jetty/send!
          ws
          (html5 (chat-pages/show-chat-login {:hx-swap-oob "true"})))
        (jetty/close! ws)))))

(defn on-text [{:keys [path-params] :as context} _ws text-message]
  (let [{:keys [username]} path-params
        json (u/read-json text-message)
        command (keyword (get-in json [:HEADERS :HX-Trigger-Name]))]
    (commands/handle-command context (-> json
                                         (assoc
                                           :username username
                                           :command command)
                                         (dissoc :HEADERS)))))

(defn on-close [{:keys [users path-params] :as context} _ws _status-code _reason]
  (let [{:keys [username]} path-params
        _ (log/debugf "on-close triggered for user: %s" username)
        _ (remove-user! users username)
        command {:command :leave-chat :username username}]
    (commands/handle-command context command)))

(defn on-error [{:keys [users path-params] :as context} _ws err]
  (let [{:keys [username]} path-params
        _ (log/debugf "on-error triggered for user: %s" username)
        _ (remove-user! users username)
        command {:command :leave-chat :username username}]
    (commands/handle-command context command)
    (println err)))

(defn ws-upgrade-handler [context upgrade-request]
  (let [provided-subprotocols (:websocket-subprotocols upgrade-request)
        provided-extensions (:websocket-extensions upgrade-request)]
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

(defn landing-page-handler [_] (ok chat-pages/landing-page))

(defn get-chatroom-page-handler [{:keys [params] :as request}]
  (let [{:keys [username room-name]
         :or   {username "TESTUSER" room-name "TESTROOM"}} params
        args {:username username :room-name room-name}]
    (ok (chat-pages/wrap-as-page
          (chat-pages/chat-page
            (update request :params merge args))))))

(defn post-chatroom-page-handler [request]
  (ok (chat-pages/chat-page request)))

(def routes
  [["/chat" {:handler landing-page-handler}]
   ["/chat/ws/:room-name/:username" {:handler    ws-handler
                                     :parameters {:path {:room-name string?
                                                         :username  string?}}}]
   ["/chat/room" {:get  get-chatroom-page-handler
                  :post post-chatroom-page-handler}]])