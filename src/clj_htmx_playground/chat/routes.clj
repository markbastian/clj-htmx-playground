(ns clj-htmx-playground.chat.routes
  (:require
    [clj-htmx-playground.chat.api.user-manager-api :as user-manager-api]
    [clj-htmx-playground.chat.domain :as chat]
    [clj-htmx-playground.chat.commands :as commands]
    [clj-htmx-playground.chat.pages :as chat-pages]
    [clj-htmx-playground.utils :as u]
    [clojure.pprint :as pp]
    [clojure.tools.logging :as log]
    [ring.adapter.jetty9 :as jetty]
    [ring.util.http-response :refer [ok internal-server-error]]
    [clj-htmx-playground.ws-handlers :as ws-handlers]
    [hiccup.page :refer [html5]]
    [clj-htmx-playground.chat.api.user :as sender]))

(defn on-connect [{:keys [user-manager path-params] :as context} ws]
  (let [{:keys [username room-name]} path-params]
    (if (user-manager-api/add-user! user-manager (sender/ws-user username ws))
      (commands/handle-command context {:command   :change-room
                                        :username  username
                                        :room-name room-name})
      (do
        (jetty/send!
          ws
          (html5 (chat-pages/show-chat-login {:hx-swap-oob "true"})))
        (jetty/close! ws)))))

(defn on-text [{:keys [path-params] :as context} _ws text-message]
  (pp/pprint (select-keys context [:params :path-params :parameters]))
  (let [{:keys [username]} path-params
        json (u/read-json text-message)
        command (keyword (get-in json [:HEADERS :HX-Trigger-Name]))]
    (commands/handle-command context (-> json
                                         (assoc
                                           :username username
                                           :command command)
                                         (dissoc :HEADERS)))))

(defn on-close [{:keys [user-manager path-params] :as context} ws _status-code _reason]
  (let [{:keys [username]} path-params
        tx (user-manager-api/remove-user! user-manager username)]
    (log/debugf "on-close triggered for user: %s" username)
    (chat/leave-chat user-manager tx username)))

(defn on-error [{:keys [user-manager path-params] :as context} ws err]
  (let [{:keys [username]} path-params
        tx (user-manager-api/remove-user! user-manager username)]
    (log/debugf "on-error triggered for user: %s" username)
    (chat/leave-chat user-manager tx username)
    (println ws)
    (println err)
    (println "ERROR")))

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