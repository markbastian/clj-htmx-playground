(ns clj-htmx-playground.chat.events
  (:require [clj-htmx-playground.chat.htmx-events :as htmx-events]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]))

;; TODO - How do we handle event dispatch for different transports?
;; This doesn't really buy us much when you are all in on one client type, but
;; what about if you have htmx, swing, and tcp clients?
;; This needs to be inverted to a subscriber model.

(defmulti handle-event (fn [{:keys [transform]} {:keys [event]}]
                         [transform event]))

(defmethod handle-event :default [{:keys [transform]} {:keys [event] :as evt}]
  (log/warn "Unhandled event!")
  (pp/pprint
    {:dispatch [transform event]
     :event    evt}))

(defmethod handle-event [:htmx :user-left-room] [{:keys [clients db]}
                                                 {:keys [username room-name]}]
  (htmx-events/broadcast-leave-room clients db username room-name))

(defmethod handle-event [:htmx :user-entered-room] [{:keys [clients db]}
                                                    {:keys [username room-name]}]
  (htmx-events/update-room-link (clients username) room-name)
  (htmx-events/update-chat-prompt (clients username) room-name)
  (htmx-events/broadcast-enter-room clients db username room-name))

(defmethod handle-event [:htmx :room-created] [{:keys [clients db]} _]
  (htmx-events/broadcast-update-room-list clients db))

(defmethod handle-event [:htmx :room-deleted] [{:keys [clients db]} _]
  (htmx-events/broadcast-update-room-list clients db))

(defmethod handle-event [:htmx :user-joined-chat] [{:keys [clients db]} _]
  (htmx-events/broadcast-update-user-list clients db))

(defmethod handle-event [:htmx :user-left-chat] [{:keys [clients db]} _]
  (htmx-events/broadcast-update-user-list clients db))

(defmethod handle-event [:htmx :chat-message-created] [{:keys [clients db]}
                                                       {:keys [username room-name chat-message]}]
  (htmx-events/update-chat-prompt (clients username) room-name)
  (htmx-events/broadcast-chat-message clients db username room-name chat-message))