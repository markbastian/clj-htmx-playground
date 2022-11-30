(ns clj-htmx-playground.middleware)

(defn wrap-component [handler component]
  (fn [request] (handler (into component request))))

