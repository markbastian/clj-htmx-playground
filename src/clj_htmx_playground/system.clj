(ns clj-htmx-playground.system
  (:require
    [clojure.tools.logging :as log]
    [datascript.core :as d]
    [parts.ring.adapter.jetty9.core :as jetty9]
    [integrant.core :as ig]
    [clj-htmx-playground.web :as web]))

;;https://github.com/markbastian/conj2019/blob/master/src/main/clj/conj2019/full_demo/web/v0.clj
;; https://arhamjain.com/2021/11/22/nim-simple-chat.html
; https://www.w3schools.com/howto/howto_js_collapse_sidebar.asp
; https://www.w3schools.com/howto/howto_css_sidebar_responsive.asp
; https://codeopinion.com/commands-events-whats-the-difference/
; https://hackernoon.com/the-3-levels-of-clean-event-driven-architecture

; Someday
;(def clients-schema {:ws        {:db/unique :db.unique/identity}
;                   :username  {:db/unique :db.unique/identity}})

(def chat-schema {:username {:db/unique :db.unique/identity}})

(def games (atom {}))

;; TODO - Change to a dsdb
(defmethod ig/init-key ::clients [_ _]
  (log/debug "Creating user map")
  (atom {}))

(defmethod ig/init-key ::conn [_ {:keys [schema]}]
  (log/debug "Creating in-memory datascript connection.")
  (d/create-conn schema))

(defmethod ig/init-key ::games [_ _]
  (log/debug "Creating in-memory games map.")
  games)

(defmethod ig/halt-key! ::games [_ _]
  (log/debug "Removing in-memory games map.")
  (reset! games {}))

(def config
  {::conn          {:schema chat-schema}
   ::clients       {}
   ::games         {}
   ::jetty9/server {:host             "0.0.0.0"
                    :port             3000
                    :join?            false
                    :clients          (ig/ref ::clients)
                    ;; TODO - Rename to chat-db
                    :conn             (ig/ref ::conn)
                    :games            (ig/ref ::games)
                    :ws-max-idle-time (* 10 60 1000)
                    :handler          #'web/handler}})

(defonce ^:dynamic *system* nil)

(defn system [] *system*)

(defn start []
  (alter-var-root #'*system* (fn [s] (if-not s (ig/init config) s))))

(defn stop []
  (alter-var-root #'*system* (fn [s] (when s (ig/halt! s) nil))))

(defn restart [] (stop) (start))

(comment
  (start)
  (stop)
  (restart)
  (system)

  (let [conn (::conn (system))]
    @conn)

  (let [clients (::clients (system))]
    clients)

  (let [conn (::conn (system))]
    (d/entity @conn [:username "C"]))

  (let [games (::games (system))]
    games)

  (let [{:keys [db-before db-after]} (-> (d/empty-db chat-schema)
                                         (d/db-with [{:username "A" :room-name "RA"}
                                                     {:username "B" :room-name "RB"}])
                                         (d/with [[:db/retractEntity [:username "A"]]]))
        room (:room-name (d/entity db-before [:username "A"]))]
    (d/q
      '[:find ?e . :in $ ?room-name :where [?e :room-name ?room-name]]
      db-after room))
  )