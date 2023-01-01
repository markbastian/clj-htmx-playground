(ns clj-htmx-playground.chat.htmx-notifications
  (:require
    [clj-htmx-playground.chat.pages :as chat-pages]
    [clj-htmx-playground.chat.queries :as chat-queries]
    [sca.api.client :as client-api]
    [hiccup.page :refer [html5]]))

(defn all-usernames-html [db]
  (->> (chat-queries/usernames db)
       sort
       (map chat-pages/sidebar-sublist-item)))

(defn update-chat-prompt [user room-name]
  (let [message (html5 (chat-pages/chat-prompt room-name {:autofocus "true" :hx-swap-oob "true"}))]
    (client-api/send! user message)))

(defn update-room-link [user room-name]
  (let [message (html5 (chat-pages/room-change-link room-name {:hx-swap-oob "true"}))]
    (client-api/send! user message)))

(defn broadcast-update-room-list [clients db]
  (let [data (chat-queries/rooms db)
        message (->> data
                     chat-pages/occupied-rooms-list
                     (chat-pages/sidebar-sublist {:id "roomList"})
                     html5)
        usernames (chat-queries/usernames db)]
    (client-api/broadcast! clients usernames message)))

(defn broadcast-update-user-list [clients db]
  (let [html (chat-pages/sidebar-sublist {:id "userList"} (all-usernames-html db))
        message (html5 html)
        usernames (chat-queries/usernames db)]
    (client-api/broadcast! clients usernames message)))

(defn broadcast-to-room [clients db room-name message]
  (let [usernames (chat-queries/users-in-room db room-name)
        message (html5
                  (chat-pages/notifications-pane
                    {:hx-swap-oob "beforeend"}
                    [:div [:i message]]))]
    (client-api/broadcast! clients usernames message)))

(defn broadcast-enter-room [clients db username new-room-name]
  (let [message (format "%s joined %s" username new-room-name)]
    (broadcast-to-room clients db new-room-name message)))

(defn broadcast-leave-room [clients db username old-room-name]
  (let [message (format "%s left %s" username old-room-name)]
    (broadcast-to-room clients db old-room-name message)))

(defn broadcast-chat-message [clients db username room-name message]
  (let [message (format "%s: %s" username message)]
    (broadcast-to-room clients db room-name message)))