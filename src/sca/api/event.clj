(ns sca.api.event
  (:require
    [clojure.pprint :as pp]
    [clojure.tools.logging :as log]))

(defmulti process-event (fn [{:keys [transform]} {:keys [event]}]
                         [transform event]))

(defmethod process-event :default [{:keys [transform]} {:keys [event] :as evt}]
  (log/warn "Unhandled event!")
  (pp/pprint
    {:dispatch [transform event]
     :event    evt}))