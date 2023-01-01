(ns clj-htmx-playground.chat.commands
  (:require [clj-htmx-playground.chat.domain :as chat]
            [clj-htmx-playground.chat.events :as events]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]))

(defmulti handle-command (fn [_ctx {:keys [command]}] command))

(defmethod handle-command :default [_ {:keys [command] :as cmd}]
  (log/warnf
    "Unhandled command: %s\n%s"
    command
    (with-out-str (pp/pprint cmd))))

(defmethod handle-command :chat-message [context {:keys [username chat-message]}]
  (chat/create-chat-message (update context :clients deref) username chat-message))

(defmethod handle-command :change-room [context {:keys [username room-name]}]
  (chat/join-room (update context :clients deref) username room-name))

(defmethod handle-command :leave-chat [context {:keys [username]}]
  (chat/leave-chat (update context :clients deref) username))

(defn sync-handler [{:keys [clients] :as context} command]
  (let [events (handle-command context command)]
    (doseq [event events]
      (events/handle-event context event))))
