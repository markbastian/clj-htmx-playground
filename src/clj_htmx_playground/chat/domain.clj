(ns clj-htmx-playground.chat.domain
  (:require
    [clj-htmx-playground.chat.queries :as chat-queries]
    [clj-htmx-playground.chat.htmx-events :as htmx-events]
    [datascript.core :as d]))

;; These are the real actions that can occur.
;; Consider -- Do all things with a user or users arg need to be downstream
;; effects? How do I handle the "different transport" issue? E.g. how do
;; update-chat-prompt & update-room-link change for TCP vs htmx vs SPA?
;; TODO - Refine conditional logic on some of the broadcasts, esp. if events are
;; generated.
;; Actually, I've got this wrong. The htmx specific derivations of these items
;; are subscriptions to events. Each subscriber of a given type knows how to
;; handle events in their own way. In order to make this work, I should put all
;; events in a ns then the users (perhaps these should be subscribers or
;; connections) will all interpret the events rather than the events being
;; pushed out.

(defn create-chat-message [{:keys [users conn]} username chat-message]
  ;; We _should_ track the message in a queue or something and then these items
  ;; are just effects.
  (let [room-name (chat-queries/current-room @conn username)]
    (htmx-events/update-chat-prompt (users username) room-name)
    (htmx-events/broadcast-chat-message users @conn username room-name chat-message)))

(defn join-room [{:keys [users conn]} username room-name]
  (let [{old-room-name :room-name} (d/entity @conn [:username username])
        user (users username)]
    (when-not (= room-name old-room-name)
      (let [db (:db-after (d/transact! conn [{:username username :room-name room-name}]))]
        (htmx-events/update-room-link user room-name)
        (htmx-events/update-chat-prompt user room-name)
        (htmx-events/broadcast-leave-room users db username old-room-name)
        (htmx-events/broadcast-enter-room users db username room-name)
        (htmx-events/broadcast-update-room-list users db)
        (htmx-events/broadcast-update-user-list users db)))))

(defn leave-chat [{:keys [users conn]} username]
  (let [room-name (chat-queries/current-room @conn username)
        tx-data [[:db/retractEntity [:username username]]]
        db (:db-after (d/transact! conn tx-data))]
    ;; Events
    (htmx-events/broadcast-leave-room users db username room-name)
    (htmx-events/broadcast-update-room-list users db)
    (htmx-events/broadcast-update-user-list users db)))

