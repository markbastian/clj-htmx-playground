(ns clj-htmx-playground.chat.api.user-manager-api)

(defprotocol IUserManager
  (add-user! [_ user-record])
  (remove-user! [_ username])
  (get-user [_ username]))