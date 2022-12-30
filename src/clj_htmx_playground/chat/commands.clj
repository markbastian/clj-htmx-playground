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

;(defmethod handle-command :join-chat [{:keys [path-params] :as context}
;                                      {:keys [username] :as _command}]
;  (chat/join-room context username room-name))

(defmethod handle-command :chat-message [{:keys [user-manager conn] :as _context}
                                         {:keys [username chat-message] :as _cmd}]
  (let [db @conn
        {:keys [room-name]} (d/entity db [:username username])]
    (chat/update-chat-prompt user-manager db username)
    (chat/broadcast-chat-message user-manager db username room-name chat-message)))

(defmethod handle-command :change-room [context
                                        {:keys [username room-name] :as _cmd}]
  (chat/join-room context username room-name))