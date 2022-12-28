(ns clj-htmx-playground.decoder.pages
  (:require
    [clojure.java.io :as io]
    [clojure.pprint :as pp]
    [clojure.string :as str]
    [hiccup.page :refer [html5 include-css include-js]]))

(defonce words4
         (with-open [r (io/reader (io/resource "clj_htmx_playground/english.txt"))]
           (->> (line-seq r)
                (map str/upper-case)
                (filter (fn [s] (re-matches #"[A-Z]+" s)))
                (filter #(= 4 (count %)))
                vec)))

(defn generate-game-name []
  (vec (take 2 (shuffle words4))))

(defn wrap-as-page [content]
  (html5
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    content))

(def join-game-template
  [:form#app.container
   [:div.d-grid.gap-2
    [:h4.text-center "Sneaky Words"]
    #_[:div.alert.alert-warning.alert-dismissible.fade.show {:role "alert"}
       [:strong "Holy guacamole!"] "You should check in on some of those fields below."
       [:button.btn-close {:type "button" :data-bs-dismiss "alert" :aria-label "Close"}]]
    [:input.form-control
     {:name         "username"
      :placeholder  "Enter username"
      :autocomplete "off"}]
    [:p.text-center.font-weight-bold "Join an existing game"]
    [:input.form-control
     {:name         "game-name"
      :placeholder  "Enter existing game name"
      :autocomplete "off"}]
    [:button.btn.btn-primary.btn-dark
     {:hx-trigger "click"
      :hx-post    "/joinGame"
      :hx-target  "#app"}
     "Join Game"]
    [:p.text-center.font-weight-bold "or"]
    [:button.btn.btn-primary.btn-dark
     {:hx-trigger "click"
      :hx-post    "/createGame"
      :hx-target  "#app"}
     "Create Game"]]])

(defn join-game [{:keys [games] :as context}]
  (wrap-as-page join-game-template))

(defn encryptor-template [game-state team]
  (let [{:keys [words card clues]} (get-in game-state [team])
        team-name (name team)
        class-suffix ({:white "light" :black "dark"} team)
        text-class (format "text-bg-%s" class-suffix)
        btn-class (format "btn-%s" class-suffix)]
    (if (seq clues)
      [:div
       {:id (format "%s-clues" team-name)}
       [:h5 "You are the encryptor. You provided these clues:"]
       [:form.form-group
        (for [i (range (count card))
              :let [card-index (card i)
                    clue (clues i)]]
          [:div.input-group.mb-3
           [:span.input-group-text {:class text-class} card-index]
           [:input.form-control
            {:class    text-class
             :type     "text"
             :value    clue
             :readonly "true"}]])]]
      [:div
       {:id (format "%s-clues" team-name)}
       [:h5 "You are the encryptor. Provide 3 clues:"]
       [:form.form-group
        (for [i (range (count card))
              :let [card-index (card i)]]
          [:div.input-group.mb-3
           [:span.input-group-text {:class text-class} card-index]
           [:input.form-control
            {:class        text-class
             :type         "text"
             :id           (format "%s-word-%s" team-name i)
             :name         (format "%s-clue-%s" team-name i)
             :placeholder  (format "Enter Clue for %s" (get-in words [(dec card-index) :text]))
             :autocomplete "off"}]])
        [:button.btn
         {:class   btn-class
          :type    "button"
          :hx-post "/submitClues"}
         "Submit Clues"]]])))

(defn guessor-template [game-state team]
  (let [{:keys [clues]} (get-in game-state [team])
        team-name (name team)
        class-suffix ({:white "light" :black "dark"} team)
        text-class (format "text-bg-%s" class-suffix)
        btn-class (format "btn-%s" class-suffix)]
    [:div
     {:id (format "%s-clues" team-name)}
     [:h5 "Clues:"]
     [:form.form-group
      (for [i (range (count clues)) :let [clue-index (clues i)]]
        [:div.input-group.mb-3
         [:div.d-grid.gap-2
          [:button.btn.btn-dark
           {:type      "submit"
            :class     text-class
            :draggable "true"
            ;:hx-post   "/chat"
            ;:hx-target "#app"
            ;:hx-vals   (u/to-json-str {:room-name "public"})
            }
           (clues i)]]])
      [:button.btn
       {:class   btn-class
        :type    "button"
        :hx-post "/submitGuess"}
       "Submit Guess"]]]))

(defn word-cards-template [game-state player-name team]
  (let [{:keys [words players]} (get-in game-state [team])
        team-name (name team)
        class-suffix ({:white "light" :black "dark"} team)
        text-class (format "text-bg-%s" class-suffix)]
    [:div.row
     (for [i (range (count words))
           :let [{:keys [text clues] :as _word} (words i)]]
       [:div.col
        {:id (format "%s-word-%s" team-name i)}
        [:div.card.text-center.mb-3
         {:class text-class :style "max-width: 18rem;"}
         [:div.card-header
          (if (get players player-name)
            text
            (format "Word %s" (inc i)))]
         [:ul.list-group.list-group-flush
          (for [clue clues]
            [:li.list-group-item
             {:class text-class}
             (if (seq clue) clue "--")])]]])]))

(defn team-template [game-state player-name team]
  (let [{:keys [encryptor]} (game-state team)
        team-name (name team)]
    [:div.container
     [:p.fs-4 (format "Team %s" (str/capitalize team-name))]
     (word-cards-template game-state player-name team)
     (if (= player-name encryptor)
       (encryptor-template game-state team)
       (guessor-template game-state team))]))

(defn cards [{:keys [games game-name player-name]}]
  (let [game-state (games game-name)]
    (let [players->team
          (merge
            (zipmap (get-in game-state [:black :players]) (repeat :black))
            (zipmap (get-in game-state [:white :players]) (repeat :white)))
          team (players->team player-name)
          other-team ({:white :black :black :white} team)]
      (pp/pprint game-state)
      (pp/pprint players->team)
      (html5
        (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
        (include-js
          "https://unpkg.com/htmx.org@1.8.4"
          "https://unpkg.com/htmx.org/dist/ext/ws.js"
          "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
        [:div#app
         [:h4.text-center (format "Game name: %s" game-name)]
         (team-template game-state player-name team)
         (team-template game-state player-name other-team)]))))