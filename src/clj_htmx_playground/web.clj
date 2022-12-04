(ns clj-htmx-playground.web
  (:require
    [clojure.pprint :as pp]
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
    [clj-htmx-playground.examples.sidebar :as sidebar]
    [clj-htmx-playground.examples.bootstrap-sidebar :as bootstrap-sidebar]
    [clj-htmx-playground.chat.domain :as chat]
    [clj-htmx-playground.chat.pages :as chat-pages]
    [clj-htmx-playground.decoder.pages :as decoder-pages]
    [clj-htmx-playground.examples.modal :as modal]
    [clj-htmx-playground.examples.offcanvas :as offcanvas]
    [clj-htmx-playground.examples.tailwind.simple :as tw.simple]
    [clj-htmx-playground.examples.bootstrap.flex :as bs.flex]))

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
     :on-close    (fn on-close [_ws _status-code _reason]
                    (chat/leave-chat context username))
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
  (reduce
    into
    [["/" {:handler (fn [_] (ok chat-pages/landing-page))}]
     ["/ws/:room-name/:username" {:handler    ws-handler
                                  :parameters {:path {:room-name string?
                                                      :username  string?}}}]
     ["/chat" {:get  (fn [request] (ok (chat-pages/wrap-as-page
                                         (chat-pages/chat-page
                                           (update request
                                                   :params
                                                   merge
                                                   {:username "Mark"
                                                    :roomname "public"})))))
               :post (fn [request] (ok (chat-pages/chat-page request)))}]
     ["/chatMessage" {:post (fn [request] (ok (chat-pages/post-chat-message request)))}]
     ["/cards" {:handler (fn [request] (ok decoder-pages/cards))}]
     ["/submitClues" {:post (fn [request]
                              (pp/pprint (select-keys request [:params
                                                               :parameters
                                                               :form-params
                                                               :path-params
                                                               :query-params]))
                              (ok "Foo"))}]
     ["/sb" {:handler (fn [_] (ok bootstrap-sidebar/sidebar))}]
     ["/offcanvas" {:handler (fn [_] (ok offcanvas/offcanvas))}]
     ["/tailwind"
      ["/simple" {:handler (fn [_] (ok tw.simple/page))}]]
     ["/bootstrap"
      ["/flex" {:handler (fn [_] (ok bs.flex/flex))}]]]
    [sidebar/routes
     modal/routes]))

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