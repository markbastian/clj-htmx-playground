(ns clj-htmx-playground.chat.domain
  (:require
    [clj-htmx-playground.chat.pages :as chat-pages]
    [clojure.pprint :as pp]
    [datascript.core :as d]
    [clj-htmx-playground.utils :as u]
    [hiccup.page :refer [html5]]
    [ring.adapter.jetty9 :as jetty]))

(def all-rooms-query
  '[:find [?room-name ...]
    :in $
    :where
    [_ :room-name ?room-name]])

(def all-users-query
  '[:find [?username ...]
    :in $
    :where
    [_ :username ?username]])

(defn occupied-rooms [db]
  (->> (d/q all-rooms-query db)
       sort
       (map (fn [room-name]
              (chat-pages/sidebar-sublist-item
                room-name
                {:ws-send "true"
                 :name    "change-room"
                 :method  :post
                 :hx-vals (u/to-json-str {:room-name room-name})})))))

(defn all-users [db]
  (->> (d/q all-users-query db)
       sort
       (map chat-pages/sidebar-sublist-item)))

(def all-ws-query
  '[:find [?ws ...] :in $ :where [?e :ws ?ws]])

(def room-name->ws-query
  '[:find [?ws ...]
    :in $ ?room-name
    :where
    [?e :ws ?ws]
    [?e :room-name ?room-name]])

(defn update-chat-prompt [db username]
  (let [{:keys [ws room-name]} (d/entity db [:username username])
        html (chat-pages/chat-prompt room-name {:autofocus   "true"
                                                :hx-swap-oob "true"})]
    (jetty/send! ws (html5 html))))

(defn broadcast-update-room-list [db]
  (let [html (chat-pages/sidebar-sublist {:id "roomList"} (occupied-rooms db))
        room-list-html (html5 html)]
    (doseq [client (d/q all-ws-query db)]
      (jetty/send! client room-list-html))))

(defn broadcast-update-user-list [db]
  (let [html (chat-pages/sidebar-sublist {:id "userList"} (all-users db))
        room-list-html (html5 html)]
    (doseq [client (d/q all-ws-query db)]
      (jetty/send! client room-list-html))))

(defn broadcast-to-room [db room-name message]
  (let [html (chat-pages/notifications-pane
               {:hx-swap-oob "beforeend"}
               [:div [:i message]])]
    (doseq [client (d/q room-name->ws-query db room-name)]
      (jetty/send! client (html5 html)))))

(defn broadcast-enter-room [db username new-room-name]
  (let [message (format "%s joined %s" username new-room-name)]
    (broadcast-to-room db new-room-name message)))

(defn broadcast-leave-room [db username old-room-name]
  (let [message (format "%s left %s" username old-room-name)]
    (broadcast-to-room db old-room-name message)))

(defn broadcast-chat-message [db username room-name message]
  (let [message (format "%s: %s" username message)]
    (broadcast-to-room db room-name message)))

(defn join-room [{:keys [conn]} username room-name]
  (let [{old-room-name :room-name ws :ws} (d/entity @conn [:username username])]
    (when-not (= room-name old-room-name)
      (jetty/send! ws (html5
                        (chat-pages/room-change-link
                          room-name
                          {:hx-swap-oob "true"})))
      (let [{:keys [db-after]} (d/transact! conn [{:username username :room-name room-name}])]
        (broadcast-leave-room db-after username old-room-name)
        (broadcast-enter-room db-after username room-name)
        (broadcast-update-room-list db-after)
        (broadcast-update-user-list db-after)
        (update-chat-prompt db-after username)))))

(defn leave-chat [{:keys [conn]} username]
  (let [{:keys [room-name]} (d/entity @conn [:username username])
        {:keys [db-after]} (d/transact! conn [[:db/retractEntity [:username username]]])]
    (broadcast-leave-room db-after username room-name)
    (broadcast-update-room-list db-after)
    (broadcast-update-user-list db-after)))

(defmulti on-text-handler (fn [_ctx json]
                            (get-in json [:HEADERS :HX-Trigger-Name])))

(defmethod on-text-handler :default [_ json]
  (println "UNKNOWN DISPATCH VALUE")
  (pp/pprint json)
  (println "END UNKNOWN DISPATCH VALUE"))

(defmethod on-text-handler "chat-message" [{:keys [path-params conn] :as _context}
                                           {:keys [chat-message] :as _json}]
  (let [{:keys [username]} path-params
        {:keys [room-name]} (d/entity @conn [:username username])]
    (update-chat-prompt @conn username)
    (broadcast-chat-message @conn username room-name chat-message)))

(defmethod on-text-handler "change-room" [{:keys [path-params] :as context} {:keys [room-name]}]
  (let [{:keys [username]} path-params]
    (join-room context username room-name)))