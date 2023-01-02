(ns sca.api.command
  (:require
    [clojure.pprint :as pp]
    [clojure.tools.logging :as log]))

(defmulti dispatch-command (fn [_ctx {:keys [command]}] command))

(defmethod dispatch-command :default [_ {:keys [command] :as cmd}]
  (log/warnf
    "Unhandled command: %s\n%s"
    command
    (with-out-str (pp/pprint cmd))))