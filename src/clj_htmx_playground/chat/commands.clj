(ns clj-htmx-playground.chat.commands
  (:require [clj-htmx-playground.chat.domain :as chat]
            [sca.api.command :as command-api]))

(defmethod command-api/handle-command :chat-message
  [context {:keys [username chat-message]}]
  (chat/create-chat-message
    (update context :clients deref)
    username chat-message))

(defmethod command-api/handle-command :change-room
  [context {:keys [username room-name]}]
  (chat/join-room (update context :clients deref) username room-name))

(defmethod command-api/handle-command :leave-chat
  [context {:keys [username]}]
  (chat/leave-chat (update context :clients deref) username))
