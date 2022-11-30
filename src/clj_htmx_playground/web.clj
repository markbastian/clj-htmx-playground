(ns clj-htmx-playground.web
  (:require
    [datascript.core :as d]
    [jsonista.core :as j]
    [reitit.ring :as ring]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.util.http-response :refer [ok not-found]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.json :refer [wrap-json-response]]
    [ring.adapter.jetty9 :as jetty]
    [clj-htmx-playground.expand-collapse :as sidebar]
    [clj-htmx-playground.chat.domain :as chat]
    [clj-htmx-playground.chat.pages :as chat-pages]))

;;https://github.com/markbastian/conj2019/blob/master/src/main/clj/conj2019/full_demo/web/v0.clj
;; https://arhamjain.com/2021/11/22/nim-simple-chat.html
; https://www.w3schools.com/howto/howto_js_collapse_sidebar.asp
; https://www.w3schools.com/howto/howto_css_sidebar_responsive.asp

(defn ws-upgrade-handler [{:keys [path-params conn] :as context} upgrade-request]
  (let [{:keys [username room-name]} path-params
        provided-subprotocols (:websocket-subprotocols upgrade-request)
        provided-extensions (:websocket-extensions upgrade-request)]
    {:on-connect  (fn on-connect [ws]
                    (d/transact! conn [{:ws ws :username username}])
                    (chat/join-room context username room-name))
     :on-text     (fn on-text [_ws text-message]
                    (let [json (j/read-value text-message j/keyword-keys-object-mapper)]
                      (chat/on-text-handler context json)))
     :on-bytes    (fn on-bytes [_ _ _ _]
                    (println "on-bytes unhandled"))
     :on-close    (fn on-close [ws _status-code _reason]
                    (let [{:keys [room-name]} (d/entity @conn [:ws ws])
                          {:keys [db-after]} (d/transact! conn [[:db/retractEntity [:ws ws]]])]
                      (chat/broadcast-leave-room db-after username room-name)
                      (chat/broadcast-update-room-list @conn)))
     :on-ping     (fn on-ping [ws payload] (println "PING")
                    (jetty/send! ws payload))
     :on-pong     (fn on-pong [_ _] (println "PONG"))
     :on-error    (fn on-error [_ _err] (println "ERROR"))
     :subprotocol (first provided-subprotocols)
     :extensions  provided-extensions}))

(defn ws-handler
  ([request]
   (if (jetty/ws-upgrade-request? request)
     (jetty/ws-upgrade-response (partial ws-upgrade-handler request))
     {:status 200 :body "hello"}))
  ([request resp _raise]
   (resp
     (if (jetty/ws-upgrade-request? request)
       (jetty/ws-upgrade-response (partial ws-upgrade-handler request))
       {:status 200 :body "hello"}))))

(def routes
  (into
    [["/" {:handler (fn [_] (ok chat-pages/landing-page))}]
     ["/ws/:room-name/:username" {:handler    ws-handler
                                  :parameters {:path {:room-name string?
                                                      :username  string?}}}]
     ["/chat" {:post (fn [request] (ok (chat-pages/chat-room request)))}]]
    sidebar/routes))

(def handler
  (ring/ring-handler
    (ring/router
      routes
      {:data {:middleware [[wrap-defaults
                            (-> site-defaults
                                (update :security dissoc :anti-forgery)
                                (update :security dissoc :content-type-options)
                                (update :responses dissoc :content-types))]
                           ;wrap-params
                           wrap-json-response
                           parameters/parameters-middleware
                           muuntaja/format-request-middleware
                           coercion/coerce-response-middleware
                           coercion/coerce-request-middleware]}})
    (constantly (not-found "Not found"))))