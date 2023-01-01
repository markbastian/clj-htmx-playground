(ns clj-htmx-playground.chat.events
  (:require [clj-htmx-playground.chat.htmx-notifications :as htmx]
            [sca.api.event :as event-api]))

(defmethod event-api/handle-event [:htmx :user-left-room]
  [{:keys [clients db]}
   {:keys [username room-name]}]
  (htmx/broadcast-leave-room clients db username room-name))

(defmethod event-api/handle-event [:htmx :user-entered-room]
  [{:keys [clients db]}
   {:keys [username room-name]}]
  (htmx/update-room-link (clients username) room-name)
  (htmx/update-chat-prompt (clients username) room-name)
  (htmx/broadcast-enter-room clients db username room-name))

(defmethod event-api/handle-event [:htmx :room-created]
  [{:keys [clients db]} _]
  (htmx/broadcast-update-room-list clients db))

(defmethod event-api/handle-event [:htmx :room-deleted]
  [{:keys [clients db]} _]
  (htmx/broadcast-update-room-list clients db))

(defmethod event-api/handle-event [:htmx :user-joined-chat]
  [{:keys [clients db]} _]
  (htmx/broadcast-update-user-list clients db))

(defmethod event-api/handle-event [:htmx :user-left-chat]
  [{:keys [clients db]} _]
  (htmx/broadcast-update-user-list clients db))

(defmethod event-api/handle-event [:htmx :chat-message-created]
  [{:keys [clients db]}
   {:keys [username room-name chat-message]}]
  (htmx/update-chat-prompt (clients username) room-name)
  (htmx/broadcast-chat-message
    clients db username room-name chat-message))