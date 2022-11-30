(ns clj-htmx-playground.web
  (:require
    [clojure.java.io :as io]
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
    [hiccup.page :refer [html5 include-css include-js]]
    [ring.adapter.jetty9 :as jetty]
    [clj-htmx-playground.expand-collapse :as sidebar]
    [clj-htmx-playground.chat.domain :as chat]))

;;https://github.com/markbastian/conj2019/blob/master/src/main/clj/conj2019/full_demo/web/v0.clj
;; https://arhamjain.com/2021/11/22/nim-simple-chat.html
; https://www.w3schools.com/howto/howto_js_collapse_sidebar.asp
; https://www.w3schools.com/howto/howto_css_sidebar_responsive.asp

(defn show-chat-login [_]
  [:form.container
   [:div.form-group
    [:h3 "Simple Chat"]
    [:h2 "Join a room!"]
    [:label "Room"]
    [:input.form-control
     {:name         "roomname"
      :type         "text"
      :placeholder  "Enter room name"
      :autocomplete "off"}]
    [:label "Username"]
    [:input.form-control
     {:name         "username"
      :type         "text"
      :placeholder  "Enter username"
      :autocomplete "off"}]]
   [:button.btn.btn-primary
    {:type      "submit"
     :hx-post   "/chat"
     :hx-target "#app"}
    "Join"]])

(def empty-room-list
  [:div#roomList.sidebar
   {:style "width: 150px;"}])

(defn chat-room [{:keys [params] :as _request}]
  (let [{:keys [roomname username]} params]
    [:div {:hx-ext "ws" :ws-connect (format "/ws/%s/%s" roomname username)}
     empty-room-list
     [:div#chat.container
      {:style "margin-left: 150px;"}
      [:h3#roomname roomname]
      [:div
       [:p {:id "notifications"}]
       [:form {:ws-send "true" :name "chat_message" :method :post}
        [:input {:name "chat_message" :autocomplete "off"}]]]]]))

(defn landing-page [request]
  (ok
    (html5
      (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
      (include-js
        "https://unpkg.com/htmx.org@1.8.4"
        "https://unpkg.com/htmx.org/dist/ext/ws.js"
        "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
      [:style (slurp (io/resource "clj_htmx_playground/styles.css"))]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:div {:id "app"} (show-chat-login request)])))

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
  "Data-driven routes."
  (into
    [["/" {:handler landing-page}]
     ["/ws/:room-name/:username" {:handler    ws-handler
                                  :parameters {:path {:room-name string?
                                                      :username  string?}}}]
     ["/clicked" {:post (fn [_] (ok (html5 [:button.btn.btn-primary
                                            {:hx-post "/clicked"
                                             :hx-swap "outerHTML"}
                                            "Click Me Again"])))}]
     ["/chat" {:post (fn [request] (ok (html5 (chat-room request))))}]]
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