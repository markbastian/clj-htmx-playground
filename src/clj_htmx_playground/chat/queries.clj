(ns clj-htmx-playground.chat.queries
  (:require [datascript.core :as d]))

(def all-usernames-query
  '[:find [?username ...] :in $ :where [_ :username ?username]])

(def all-rooms-query
  '[:find [?room-name ...] :in $ :where [_ :room-name ?room-name]])

(def room-name->username-query
  '[:find [?username ...]
    :in $ ?room-name
    :where
    [?e :username ?username]
    [?e :room-name ?room-name]])

(defn usernames [db]
  (d/q all-usernames-query db))

(defn rooms [db]
  (d/q all-rooms-query db))

(defn users-in-room [db room-name]
  (d/q room-name->username-query db room-name))

(defn current-room [db username]
  (:room-name (d/entity db [:username username])))

(defn room-exists? [db room-name]
  (let [q '[:find ?e . :in $ ?room-name :where [?e :room-name ?room-name]]]
    (some? (d/q q db room-name))))

(defn chat-user-exists? [db username]
  (some? (d/entity db [:username username])))
