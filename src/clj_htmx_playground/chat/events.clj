(ns clj-htmx-playground.chat.events
  (:require [clj-htmx-playground.chat.htmx-events :as htmx-events]))

;; TODO - How do we handle event dispatch for different transports?
;; This doesn't really buy us much when you are all in on one client type, but
;; what about if you have htmx, swing, and tcp clients?
;; This needs to be inverted to a subscriber model.

(defmulti handle-event (fn [_ctx {:keys [event]}] event))

(defmethod handle-event :default [context event]
  (println event))

(defmethod handle-event :user-left-room [{:keys [users conn]}
                                         {:keys [username room-name]}]
  (htmx-events/broadcast-leave-room @users @conn username room-name))

(defmethod handle-event :user-entered-room [{:keys [users conn]}
                                            {:keys [username room-name]}]
  (htmx-events/broadcast-enter-room @users @conn username room-name))

(defmethod handle-event :room-emptied [{:keys [users conn]} _]
  (htmx-events/broadcast-update-room-list @users @conn))

(defmethod handle-event :user-left-chat [{:keys [users conn]} _]
  (htmx-events/broadcast-update-user-list @users @conn))