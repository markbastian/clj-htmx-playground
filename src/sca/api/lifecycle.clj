(ns sca.api.lifecycle
  (:require [sca.api.command :as command-api]
            [sca.api.event :as event-api]))

(defn sync-handler [{:keys [clients conn] :as context} command]
  ;; TODO - Precompute this
  (let [f (fn [acc {:keys [transform client-id] :as client}]
            (assoc-in acc [transform client-id] client))
        clients-by-tx (->> @clients vals (reduce f {}))
        events (command-api/dispatch-command context command)]
    (doseq [[tx clients] clients-by-tx
            event events]
      (event-api/process-event
        (assoc context
          :clients clients
          :db @conn
          :transform tx)
        event))))