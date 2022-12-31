(ns clj-htmx-playground.chat.domain
  (:require
    [clj-htmx-playground.chat.pages :as chat-pages]
    [clojure.tools.logging :as log]
    [datascript.core :as d]
    [hiccup.page :refer [html5]]
    [ring.adapter.jetty9 :as jetty]))

(def all-rooms-query
  '[:find [?room-name ...]
    :in $
    :where
    [_ :room-name ?room-name]])

(def all-usernames-query
  '[:find [?username ...] :in $ :where [?e :username ?username]])

(defn all-usernames-html [db]
  (->> (d/q all-usernames-query db)
       sort
       (map chat-pages/sidebar-sublist-item)))

(def room-name->username-query
  '[:find [?username ...]
    :in $ ?room-name
    :where
    [?e :username ?username]
    [?e :room-name ?room-name]])

(defmulti send! (fn [{:keys [transport] :as _user} _message] transport))

(defmethod send! :ws [{:keys [username ws]} message]
  (log/infof "Sending to %s via ws." username)
  (jetty/send! ws message))

(defn broadcast! [users usernames message]
  (doseq [username usernames :let [user (users username)] :when user]
    (send! user message)))

(defn update-chat-prompt [{:keys [username] :as user} db]
  (let [{:keys [room-name]} (d/entity db [:username username])
        message (html5 (chat-pages/chat-prompt room-name {:autofocus "true" :hx-swap-oob "true"}))]
    (send! user message)))

(defn broadcast-update-room-list [users db]
  (let [data (d/q all-rooms-query db)
        message (->> data
                     chat-pages/occupied-rooms-list
                     (chat-pages/sidebar-sublist {:id "roomList"})
                     html5)
        usernames (d/q all-usernames-query db)]
    (broadcast! users usernames message)))

(defn broadcast-update-user-list [users db]
  (let [html (chat-pages/sidebar-sublist {:id "userList"} (all-usernames-html db))
        message (html5 html)
        usernames (d/q all-usernames-query db)]
    (broadcast! users usernames message)))

(defn broadcast-to-room [users db room-name message]
  (let [usernames (d/q room-name->username-query db room-name)
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

(defn join-room [{:keys [users conn]} username room-name]
  (let [{old-room-name :room-name} (d/entity @conn [:username username])
        user (users username)]
    (when-not (= room-name old-room-name)
      (let [message (html5 (chat-pages/room-change-link room-name {:hx-swap-oob "true"}))]
        (send! user message))
      (let [{:keys [db-after]} (d/transact! conn [{:username username :room-name room-name}])]
        (broadcast-leave-room users db-after username old-room-name)
        (broadcast-enter-room users db-after username room-name)
        (broadcast-update-room-list users db-after)
        (broadcast-update-user-list users db-after)
        (update-chat-prompt user db-after)))))

(defn leave-chat [{:keys [users conn]} username]
  (let [tx [[:db/retractEntity [:username username]]]
        {:keys [db-before db-after]} (d/transact! conn tx)]
    (let [{:keys [room-name]} (d/entity db-before [:username username])]
      (broadcast-leave-room users db-after username room-name)
      (broadcast-update-room-list users db-after)
      (broadcast-update-user-list users db-after))))

