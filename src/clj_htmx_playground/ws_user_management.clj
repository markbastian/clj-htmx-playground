(ns clj-htmx-playground.ws-user-management
  (:require
    [clojure.tools.logging :as log]
    [datascript.core :as d]))

(defn add-user [{:keys [conn] :as _context} username ws]
  (if-not (d/entity @conn [:username username])
    (do
      (log/debugf "Adding user: %s" username)
      (d/transact! conn [{:ws ws :username username}]))
    (log/debugf "Not adding existing user: %s" username)))

(defn remove-user [{:keys [conn]} ws]
  (when-some [{:keys [username]} (d/entity @conn [:ws ws])]
    (log/debugf "Removing user: %s" username)
    (d/transact! conn [[:db/retractEntity [:username username]]])))
