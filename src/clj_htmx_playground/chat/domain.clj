(ns clj-htmx-playground.chat.domain
  (:require
    [clj-htmx-playground.chat.pages :as chat-pages]
    [clj-htmx-playground.chat.api.async-comms-api :as async-comms-api]
    [datascript.core :as d]
    [hiccup.page :refer [html5]]))

(def all-rooms-query
  '[:find [?room-name ...]
    :in $
    :where
    [_ :room-name ?room-name]])

(def all-users-query
  '[:find [?username ...] :in $ :where [?e :username ?username]])

(defn all-users-hml [db]
  (->> (d/q all-users-query db)
       sort
       (map chat-pages/sidebar-sublist-item)))

(def room-name->username-query
  '[:find [?username ...]
    :in $ ?room-name
    :where
    [?e :username ?username]
    [?e :room-name ?room-name]])

(defn update-chat-prompt [user-manager db username]
  (let [{:keys [room-name]} (d/entity db [:username username])
        message (html5 (chat-pages/chat-prompt room-name {:autofocus "true" :hx-swap-oob "true"}))]
    (async-comms-api/send! user-manager username message)))

(defn broadcast-update-room-list [user-manager db]
  (let [data (d/q all-rooms-query db)
        message (->> data
                     chat-pages/occupied-rooms-list
                     (chat-pages/sidebar-sublist {:id "roomList"})
                     html5)
        users (d/q all-users-query db)]
    (async-comms-api/broadcast! user-manager users message)))

(defn broadcast-update-user-list [user-manager db]
  (let [html (chat-pages/sidebar-sublist {:id "userList"} (all-users-hml db))
        message (html5 html)
        users (d/q all-users-query db)]
    (async-comms-api/broadcast! user-manager users message)))

(defn broadcast-to-room [user-manager db room-name message]
  (let [users (d/q room-name->username-query db room-name)
        message (html5
                  (chat-pages/notifications-pane
                    {:hx-swap-oob "beforeend"}
                    [:div [:i message]]))]
    (async-comms-api/broadcast! user-manager users message)))

(defn broadcast-enter-room [user-manager db username new-room-name]
  (let [message (format "%s joined %s" username new-room-name)]
    (broadcast-to-room user-manager db new-room-name message)))

(defn broadcast-leave-room [user-manager db username old-room-name]
  (let [message (format "%s left %s" username old-room-name)]
    (broadcast-to-room user-manager db old-room-name message)))

(defn broadcast-chat-message [user-manager db username room-name message]
  (let [message (format "%s: %s" username message)]
    (broadcast-to-room user-manager db room-name message)))

(defn join-room [{:keys [user-manager conn]} username room-name]
  (let [{old-room-name :room-name} (d/entity @conn [:username username])]
    (when-not (= room-name old-room-name)
      (let [message (html5
                      (chat-pages/room-change-link
                        room-name
                        {:hx-swap-oob "true"}))]
        (async-comms-api/send! user-manager username message))
      (let [{:keys [db-after]} (d/transact! conn [{:username username :room-name room-name}])]
        (broadcast-leave-room user-manager db-after username old-room-name)
        (broadcast-enter-room user-manager db-after username room-name)
        (broadcast-update-room-list user-manager db-after)
        (broadcast-update-user-list user-manager db-after)
        (update-chat-prompt user-manager db-after username)))))

(defn leave-chat [user-manager {:keys [db-before db-after]} username]
  (let [{:keys [room-name]} (d/entity db-before [:username username])]
    (broadcast-leave-room user-manager db-after username room-name)
    (broadcast-update-room-list user-manager db-after)
    (broadcast-update-user-list user-manager db-after)))

