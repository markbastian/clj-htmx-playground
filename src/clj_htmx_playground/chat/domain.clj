(ns clj-htmx-playground.chat.domain
  (:require
    [clj-htmx-playground.chat.queries :as chat-queries]
    [clj-htmx-playground.chat.htmx-notifications :as htmx-events]
    [datascript.core :as d]))

;; These are the real actions that can occur.
;; Consider -- Do all things with a user or clients arg need to be downstream
;; effects? How do I handle the "different transport" issue? E.g. how do
;; update-chat-prompt & update-room-link change for TCP vs htmx vs SPA?
;; TODO - Refine conditional logic on some of the broadcasts, esp. if events are
;; generated.
;; Actually, I've got this wrong. The htmx specific derivations of these items
;; are subscriptions to events. Each subscriber of a given type knows how to
;; handle events in their own way. In order to make this work, I should put all
;; events in a ns then the clients (perhaps these should be subscribers or
;; connections) will all interpret the events rather than the events being
;; pushed out.

;;TODO - Put these in a db.
(defn create-chat-message [{:keys [conn]} username chat-message]
  [{:event        :chat-message-created
    :username     username
    :room-name    (chat-queries/current-room @conn username)
    :chat-message chat-message}])

(defn join-room [{:keys [conn]} username room-name]
  (let [tx-data [{:username username :room-name room-name}]
        {:keys [db-before db-after]} (d/transact! conn tx-data)
        old-room-name (chat-queries/current-room db-before username)]
    (when-not (= room-name old-room-name)
      (let [old-room-removed? (not (chat-queries/room-exists? db-after old-room-name))
            new-room-created? (not (chat-queries/room-exists? db-before room-name))
            user-already-exists? (chat-queries/chat-user-exists? db-before username)]
        (cond->
          [(if user-already-exists?
             {:event :user-left-room :username username :room-name old-room-name}
             {:event :user-joined-chat :username username})
           {:event :user-entered-room :username username :room-name room-name}]
          old-room-removed?
          (conj {:event :room-deleted :room-name old-room-name})
          new-room-created?
          (conj {:event :room-created :room-name room-name}))))))

(defn leave-chat [{:keys [conn]} username]
  (let [tx [[:db/retractEntity [:username username]]]
        {:keys [db-before db-after]} (d/transact! conn tx)]
    (let [room-name (chat-queries/current-room db-before username)
          room-removed? (not (chat-queries/room-exists? db-after room-name))]
      (cond->
        [{:event :user-left-room :username username :room-name room-name}
         {:event :user-left-chat :username username :room-name room-name}]
        room-removed?
        (conj {:event :room-deleted :room-name room-name})))))



