(ns clj-htmx-playground.decoder.routes
  (:require [clj-htmx-playground.decoder.pages :as decoder-pages]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [ring.adapter.jetty9 :as jetty]
            [sneaky-words.domain :as swd]
            [ring.util.http-response :refer [ok internal-server-error]]
            [clj-htmx-playground.utils :as pu]
            [clj-htmx-playground.decoder.commands :as commands]))

(defn on-connect [{:keys [path-params games] :as context} ws]
  (let [{:keys [username game-name]} path-params]
    (swap! games assoc-in [game-name ws] username)
    ;(commands/join-room context username room-name)
    ))

(defn on-text [context _ws text-message]
  (let [json (pu/read-json text-message)]
    (pp/pprint json)
    ;(commands/on-text-handler context json)
    ))

(defn on-bytes [_context _ _ _ _]
  (println "on-bytes unhandled"))

(defn on-close [{:keys [path-params] :as context} _ws _status-code _reason]
  #_(let [{:keys [username]} path-params]
      (swap! games update game)
      ;(commands/leave-chat context username)
      ))

(defn on-ping [_context ws payload] (println "PING")
  (jetty/send! ws payload))

(defn on-pong [_context _ _] (println "PONG"))

(defn on-error [{:keys [path-params] :as context} ws err]
  (let [{:keys [username]} path-params]
    ;(chat/leave-chat context username)
    (println ws)
    (println err)
    (println "ERROR")))

(defn ws-upgrade-handler [context upgrade-request]
  (let [provided-subprotocols (:websocket-subprotocols upgrade-request)
        provided-extensions (:websocket-extensions upgrade-request)]
    {:on-connect  (partial #'on-connect context)
     :on-text     (partial #'on-text context)
     :on-bytes    (partial #'on-bytes context)
     :on-close    (partial #'on-close context)
     :on-ping     (partial #'on-ping context)
     :on-pong     (partial #'on-pong context)
     :on-error    (partial #'on-error context)
     :subprotocol (first provided-subprotocols)
     :extensions  provided-extensions}))

(defn ws-handler
  ([request]
   (if (jetty/ws-upgrade-request? request)
     (jetty/ws-upgrade-response (partial ws-upgrade-handler request))
     (internal-server-error "Cannot upgrade request")))
  ([request resp _raise]
   (resp (ws-handler request))))

(defn landing-page-handler [request]
  (ok (decoder-pages/join-game request)))

(defn create-game-handler [{:keys [params games] :as request}]
  (pu/print-params request)
  (let [{:keys [username]} params
        game-name (str/join " " (decoder-pages/generate-game-name))
        game (-> {}
                 (swd/join-game username)
                 swd/start-game)
        games-val (swap! games assoc game-name game)]
    (ok (decoder-pages/cards
          (assoc request
            :games games-val
            :game-name game-name
            :player-name username)))))

(defn join-game-handler [request]
  (ok (decoder-pages/join-game request)))

(defn select-clue-handler [request]
  (pu/print-params request)
  (let [game-name (decoder-pages/generate-game-name)
        game-name-str (str/join " " game-name)]
    (ok (decoder-pages/cards
          (assoc request :game-name game-name-str)))))

(defn submit-clue-handler [request]
  (pp/pprint (select-keys request [:params
                                   :parameters
                                   :form-params
                                   :path-params
                                   :query-params]))
  (ok "Foo"))

(def routes
  [
   ;["/cards" {:post (fn [request]
   ;                   (ok (decoder-pages/cards
   ;                         (assoc request :game-name "RIOT TUMS"))))
   ;           :get  (fn [{:keys [query-params] :as request}]
   ;                   (let [{:strs [player-name]
   ;                          :or   {player-name "Jo"}} query-params]
   ;                     (pp/pprint query-params)
   ;                     (ok (decoder-pages/cards
   ;                           (assoc request
   ;                             :game-name "RIOT TUMS"
   ;                             :player-name player-name)))))}]
   ["/decrypto" {:handler landing-page-handler}]
   ["/decrypto/ws/:game-name/:username" {:handler    ws-handler
                                         :parameters {:path {:game-name string?
                                                             :username  string?}}}]
   ["/createGame" {:post create-game-handler}]
   ["/joinGame" {:post join-game-handler}]
   ["/selectClue" {:post select-clue-handler}]
   ["/submitClues" {:post submit-clue-handler}]])