(ns clj-htmx-playground.chat.htmx-events
  (:require
    [clj-htmx-playground.chat.pages :as chat-pages]
    [clj-htmx-playground.chat.queries :as chat-queries]
    [clojure.tools.logging :as log]
    [hiccup.page :refer [html5]]
    [ring.adapter.jetty9 :as jetty]))

(defn all-usernames-html [db]
  (->> (chat-queries/usernames db)
       sort
       (map chat-pages/sidebar-sublist-item)))

(defmulti send! (fn [{:keys [transport] :as _user} _message] transport))

(defmethod send! :ws [{:keys [username ws]} message]
  (log/infof "Sending to %s via ws." username)
  (jetty/send! ws message))

(defn broadcast! [users usernames message]
  (doseq [username usernames :let [user (users username)] :when user]
    (send! user message)))

(defn update-chat-prompt [user room-name]
  (let [message (html5 (chat-pages/chat-prompt room-name {:autofocus "true" :hx-swap-oob "true"}))]
    (send! user message)))

(defn update-room-link [user room-name]
  (let [message (html5 (chat-pages/room-change-link room-name {:hx-swap-oob "true"}))]
    (send! user message)))

(defn broadcast-update-room-list [users db]
  (let [data (chat-queries/rooms db)
        message (->> data
                     chat-pages/occupied-rooms-list
                     (chat-pages/sidebar-sublist {:id "roomList"})
                     html5)
        usernames (chat-queries/usernames db)]
    (broadcast! users usernames message)))

(defn broadcast-update-user-list [users db]
  (let [html (chat-pages/sidebar-sublist {:id "userList"} (all-usernames-html db))
        message (html5 html)
        usernames (chat-queries/usernames db)]
    (broadcast! users usernames message)))

(defn broadcast-to-room [users db room-name message]
  (let [usernames (chat-queries/users-in-room db room-name)
        message (html5
                  (chat-pages/notifications-pane
                    {:hx-swap-oob "beforeend"}
                    [:div [:i message]]))]
    (broadcast! users usernames message)))

(defn broadcast-enter-room [users db username new-room-name]
  (let [message (format "%s joined %s" username new-room-name)]
    (broadcast-to-room users db new-room-name message)))

(defn broadcast-leave-room [users db username old-room-name]
  (let [message (format "%s left %s" username old-room-name)]
    (broadcast-to-room users db old-room-name message)))

(defn broadcast-chat-message [users db username room-name message]
  (let [message (format "%s: %s" username message)]
    (broadcast-to-room users db room-name message)))