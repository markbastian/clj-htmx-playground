(ns clj-htmx-playground.chat.api.render-preference-api)

(defprotocol IRenderPreference
  (render-preference [this]))
