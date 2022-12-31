(ns clj-htmx-playground.chat.commands
  (:require [clj-htmx-playground.chat.domain :as chat]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]
            [datascript.core :as d]))

(defmulti handle-command (fn [_ctx {:keys [command]}] command))

(defmethod handle-command :default [_ {:keys [command] :as cmd}]
  (log/warnf
    "Unhandled command: %s\n%s"
    command
    (with-out-str (pp/pprint cmd))))

(defmethod handle-command :chat-message [{:keys [users conn]}
                                         {:keys [username chat-message]}]
  (let [db @conn
        {:keys [room-name]} (d/entity db [:username username])]
    (chat/update-chat-prompt (@users username) db)
    (chat/broadcast-chat-message @users db username room-name chat-message)))

(defmethod handle-command :change-room [context {:keys [username room-name]}]
  (chat/join-room (update context :users deref) username room-name))

(defmethod handle-command :leave-chat [context {:keys [username] :as _cmd}]
  (chat/leave-chat (update context :users deref) username))

(defn handle [context command]
  (let [effects (handle-command context command)]
    ))