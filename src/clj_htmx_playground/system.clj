(ns clj-htmx-playground.system
  (:require
    [clojure.tools.logging :as log]
    [datascript.core :as d]
    [parts.ring.adapter.jetty9.core :as jetty9]
    [integrant.core :as ig]
    [clj-htmx-playground.web :as web]
    [clj-htmx-playground.chat.api.user-manager :as um]))

;;https://github.com/markbastian/conj2019/blob/master/src/main/clj/conj2019/full_demo/web/v0.clj
;; https://arhamjain.com/2021/11/22/nim-simple-chat.html
; https://www.w3schools.com/howto/howto_js_collapse_sidebar.asp
; https://www.w3schools.com/howto/howto_css_sidebar_responsive.asp

(def schema {:ws        {:db/unique :db.unique/identity}
             :user-id   {:db/unique :db.unique/identity}
             :username  {:db/unique :db.unique/identity}
             :game-name {:db/unique :db.unique/identity}})

(def games (atom {}))

(defmethod ig/init-key ::user-manager [_ _]
  (log/debug "Creating datascript user manager")
  (um/user-manager (atom {})))

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
  {::conn          {:schema schema}
   ::user-manager  {}
   ::games         {}
   ::jetty9/server {:host         "0.0.0.0"
                    :port         3000
                    :join?        false
                    :user-manager (ig/ref ::user-manager)
                    :conn         (ig/ref ::conn)
                    :games        (ig/ref ::games)
                    :handler      #'web/handler}})

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

  (let [user-manager (::user-manager (system))]
    (um/get-user user-manager "AAA")
    ;(um/add-user! user-manager {:ws 3 :username "Mark"})
    )

  (let [conn (::conn (system))]
    (d/entity @conn [:username "A"]))

  (let [conn (::conn (system))]
    (d/entity @conn [:username "C"]))

  (let [games (::games (system))]
    games)

  )