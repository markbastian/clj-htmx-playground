(ns clj-htmx-playground.chat.domain
  (:require
    [clojure.pprint :as pp]
    [datascript.core :as d]
    [jsonista.core :as j]
    [hiccup.page :refer [html5]]
    [ring.adapter.jetty9 :as jetty]))

(def oob-empty-room-list
  [:div#roomList.sidebar
   {:style "width: 150px;" :hx-swap-oob "true"}])

(def all-rooms-query
  '[:find [?room-name ...]
    :in $
    :where
    [_ :room-name ?room-name]])

(defn occupied-rooms [db]
  (let [room-names (d/q all-rooms-query db)]
    (->> room-names
         sort
         (map (fn [room-name]
                [:a {:id         room-name
                     :href       ""
                     :ws-send    "true"
                     :name       "change_room"
                     :method     :post
                     :hx-headers (j/write-value-as-string {:room-name room-name} j/keyword-keys-object-mapper)}
                 room-name])))))

(def all-ws-query
  '[:find [?ws ...] :in $ :where [?e :ws ?ws]])

(def room-name->ws-query
  '[:find [?ws ...]
    :in $ ?room-name
    :where
    [?e :ws ?ws]
    [?e :room-name ?room-name]])

(defn broadcast-update-room-list [db]
  (let [room-list-html (html5
                         (into
                           oob-empty-room-list
                           (occupied-rooms db)))]
    (doseq [client (d/q all-ws-query db)]
      (jetty/send! client room-list-html))))

(defn broadcast-enter-room [db username new-room-name]
  (doseq [client (d/q room-name->ws-query db new-room-name)]
    (jetty/send!
      client
      (html5
        [:div#notifications {:hx-swap-oob "beforeend"}
         [:div [:i (format "%s joined %s" username new-room-name)]]]))))

(defn broadcast-leave-room [db username old-room-name]
  (doseq [client (d/q room-name->ws-query db old-room-name)]
    (jetty/send!
      client
      (html5
        [:div#notifications {:hx-swap-oob "beforeend"}
         [:div [:i (format "%s left %s" username old-room-name)]]]))))

(defn join-room [{:keys [conn]} username room-name]
  (let [{old-room-name :room-name ws :ws} (d/entity @conn [:username username])]
    (when-not (= room-name old-room-name)
      (jetty/send! ws (html5 [:h3#roomname {:hx-swap-oob "true"} room-name]))
      (let [{:keys [db-after]} (d/transact! conn [{:username username :room-name room-name}])]
        (broadcast-leave-room db-after username old-room-name)
        (broadcast-enter-room db-after username room-name)
        (broadcast-update-room-list db-after)))))

(defn broadcast-chat-message [db username room-name message]
  (doseq [client (d/q room-name->ws-query db room-name)]
    (jetty/send!
      client
      (html5
        [:div
         {:id          "notifications"
          :hx-swap-oob "beforeend"}
         [:div (format "%s: %s" username message)]]))))

(defmulti on-text-handler (fn [_ctx json] (get-in json [:HEADERS :HX-Trigger-Name])))

(defmethod on-text-handler :default [_ json]
  (println "UNKNOWN DISPATCH VALUE")
  (pp/pprint json)
  (println "END UNKNOWN DISPATCH VALUE"))

(defmethod on-text-handler "chat_message" [{:keys [path-params conn] :as _context}
                                           {:keys [chat_message] :as _json}]
  (let [{:keys [username]} path-params
        {:keys [room-name]} (d/entity @conn [:username username])]
    (broadcast-chat-message @conn username room-name chat_message)))

(defmethod on-text-handler "change_room" [{:keys [path-params] :as context} json]
  (let [{:keys [username]} path-params
        room-name (get-in json [:HEADERS :room-name])]
    (join-room context username room-name)))