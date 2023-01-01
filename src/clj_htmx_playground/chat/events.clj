(ns clj-htmx-playground.chat.events
  (:require [clj-htmx-playground.chat.htmx-events :as htmx-events]
            [sca.api.event :as event-api]))

(defmethod event-api/handle-event [:htmx :user-left-room]
  [{:keys [clients db]}
   {:keys [username room-name]}]
  (htmx-events/broadcast-leave-room clients db username room-name))

(defmethod event-api/handle-event [:htmx :user-entered-room]
  [{:keys [clients db]}
   {:keys [username room-name]}]
  (htmx-events/update-room-link (clients username) room-name)
  (htmx-events/update-chat-prompt (clients username) room-name)
  (htmx-events/broadcast-enter-room clients db username room-name))

(defmethod event-api/handle-event [:htmx :room-created]
  [{:keys [clients db]} _]
  (htmx-events/broadcast-update-room-list clients db))

(defmethod event-api/handle-event [:htmx :room-deleted]
  [{:keys [clients db]} _]
  (htmx-events/broadcast-update-room-list clients db))

(defmethod event-api/handle-event [:htmx :user-joined-chat]
  [{:keys [clients db]} _]
  (htmx-events/broadcast-update-user-list clients db))

(defmethod event-api/handle-event [:htmx :user-left-chat]
  [{:keys [clients db]} _]
  (htmx-events/broadcast-update-user-list clients db))

(defmethod event-api/handle-event [:htmx :chat-message-created]
  [{:keys [clients db]}
   {:keys [username room-name chat-message]}]
  (htmx-events/update-chat-prompt (clients username) room-name)
  (htmx-events/broadcast-chat-message
    clients db username room-name chat-message))