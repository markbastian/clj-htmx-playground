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

(defmethod handle-command :chat-message [{:keys [path-params conn] :as _context}
                                         {:keys [chat-message] :as _command}]
  (let [db @conn
        {:keys [username]} path-params
        {:keys [room-name]} (d/entity db [:username username])]
    (chat/update-chat-prompt db username)
    (chat/broadcast-chat-message db username room-name chat-message)))

(defmethod handle-command :change-room [{:keys [path-params] :as context}
                                        {:keys [room-name] :as _command}]
  (let [{:keys [username]} path-params]
    (chat/join-room context username room-name)))