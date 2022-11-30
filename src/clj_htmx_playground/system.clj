(ns clj-htmx-playground.system
  (:require
    [datascript.core :as d]
    [parts.ring.adapter.jetty9.core :as jetty9]
    [integrant.core :as ig]
    [clj-htmx-playground.web :as web]))

;;https://github.com/markbastian/conj2019/blob/master/src/main/clj/conj2019/full_demo/web/v0.clj
;; https://arhamjain.com/2021/11/22/nim-simple-chat.html
; https://www.w3schools.com/howto/howto_js_collapse_sidebar.asp
; https://www.w3schools.com/howto/howto_css_sidebar_responsive.asp

(def schema {:ws       {:db/unique :db.unique/identity}
             :username {:db/unique :db.unique/identity}})

(def config
  {::jetty9/server {:host    "0.0.0.0"
                    :port    3000
                    :join?   false
                    :conn    (d/create-conn schema)
                    :handler #'web/handler}})

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
  (restart))