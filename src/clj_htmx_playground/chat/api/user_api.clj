(ns clj-htmx-playground.chat.api.user-api)

(defprotocol IUser
  (send! [_ message]))
