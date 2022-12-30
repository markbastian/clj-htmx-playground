(ns clj-htmx-playground.chat.api.user-manager
  (:require [clj-htmx-playground.chat.api.user-manager-atom-impl
             :as user-manager-atom-impl]))

(defn user-manager [state]
  (user-manager-atom-impl/->AtomicUserManager state))
