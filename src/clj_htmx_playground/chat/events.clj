(ns clj-htmx-playground.chat.events
  (:require [clj-htmx-playground.chat.htmx-events :as htmx-events]))

;; TODO - How do we handle event dispatch for different transports?
;; This doesn't really buy us much when you are all in on one client type, but
;; what about if you have htmx, swing, and tcp clients?
;; This needs to be inverted to a subscriber model.

(defmulti handle-event (fn [_ctx {:keys [event]}] event))

(defmethod handle-event :default [_context event]
  (println event))

(defmethod handle-event :user-left-room [{:keys [clients conn]}
                                         {:keys [username room-name]}]
  (htmx-events/broadcast-leave-room @clients @conn username room-name))

(defmethod handle-event :user-entered-room [{:keys [clients conn]}
                                            {:keys [username room-name]}]
  (htmx-events/update-room-link (@clients username) room-name)
  (htmx-events/update-chat-prompt (@clients username) room-name)
  (htmx-events/broadcast-enter-room @clients @conn username room-name))

(defmethod handle-event :room-created [{:keys [clients conn]} _]
  (htmx-events/broadcast-update-room-list @clients @conn))

(defmethod handle-event :room-deleted [{:keys [clients conn]} _]
  (htmx-events/broadcast-update-room-list @clients @conn))

(defmethod handle-event :user-joined-chat [{:keys [clients conn]} _]
  (htmx-events/broadcast-update-user-list @clients @conn))

(defmethod handle-event :user-left-chat [{:keys [clients conn]} _]
  (htmx-events/broadcast-update-user-list @clients @conn))

(defmethod handle-event :chat-message-created [{:keys [clients conn]}
                                               {:keys [username room-name chat-message]}]
  (htmx-events/update-chat-prompt (@clients username) room-name)
  (htmx-events/broadcast-chat-message @clients @conn username room-name chat-message))