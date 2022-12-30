(ns clj-htmx-playground.chat.api.user-manager-atom-impl
  (:require
    [clojure.tools.logging :as log]
    [clj-htmx-playground.chat.api.user-api :as user-api]
    [clj-htmx-playground.chat.api.user-manager-api :as user-manager-api]
    [clj-htmx-playground.chat.api.async-comms-api :as async-comms-api]))

(defrecord AtomicUserManager [state])

(extend-type AtomicUserManager
  user-manager-api/IUserManager
  (get-user [{:keys [state]} username]
    (@state username))
  (add-user! [{:keys [state] :as this} {:keys [username] :as m}]
    (if-not (user-manager-api/get-user this username)
      (do
        (log/debugf "Adding user: %s" username)
        (swap! state assoc username m))
      (log/debugf "Adding user: %s" username)))
  (remove-user! [{:keys [state] :as this} username]
    (when-some [{:keys [username]} (user-manager-api/get-user this username)]
      (log/debugf "Removing user: %s" username)
      (swap! state dissoc username)))
  async-comms-api/IAsyncComms
  (send! [this username message]
    (log/infof "Sending to %s via ws." username)
    (user-api/send! (user-manager-api/get-user this username) message))
  (broadcast! [this usernames message]
    (doseq [username usernames :when username]
      (async-comms-api/send! this username message))))
