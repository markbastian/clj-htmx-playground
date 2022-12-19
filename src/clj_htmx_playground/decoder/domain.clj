(ns clj-htmx-playground.decoder.domain
  (:require [malli.generator :as mg]))

;;https://www.fgbradleys.com/rules/rules6/Decrypto%20-%20rules.pdf

(def state
  {:black {:players           []
           :words             [{:text "Cowboy" :clues []}
                               {:word "Mexico" :clues []}
                               {:word "Starcraft" :clues []}
                               {:word "Horse" :clues []}]
           :card              []
           :guess             []
           :intercept-guess   []
           :clues             []
           :miscommunications 0
           :intercepts        0
           :codemaster-index  0}
   :white {:players           []
           :words             [{:text "Banana" :clues []}
                               {:word "Clojure" :clues []}
                               {:word "Monkey" :clues []}
                               {:word "Hacker" :clues []}]
           :card              []
           :guess             []
           :intercept-guess   []
           :clues             []
           :miscommunications 0
           :intercepts        0
           :codemaster-index  0}})

;; Predicates
(defn can-resolve? [state team]
  (every? (comp #{3} count (state team)) [:guess :card :intercept-guess]))

(defn can-provide-clues? [state team player-index]
  (let [{:keys [codemaster-index card clues]} (state team)]
    (and
      ;;Need to ensure on same team, too!
      (= player-index codemaster-index)
      (-> card count #{3})
      (empty? clues))))

(defn can-guess? [state team player-index]
  (let [{:keys [codemaster-index card guess]} (state team)]
    (and
      ;;Need to ensure on same team, too!
      (not= player-index codemaster-index)
      (-> card count #{3})
      (empty? guess))))

(defn can-intercept-guess? [state team player-index]
  (let [{other-card :card} (state ({:white :black :black :white} team))
        {:keys [codemaster-index intercept-guess]} (state team)]
    (and
      ;;Need to ensure on same team, too!
      (not= player-index codemaster-index)
      (-> other-card count #{3})
      (empty? intercept-guess))))

;; Utilities
(defn generate-card []
  (vec (take 3 (shuffle [1 2 3 4]))))

(defn advance-codemaster [state team]
  (update-in state [team :codemaster-index]
             (fn [idx]
               (mod
                 (inc (or idx -1))
                 (count (get-in state [team :players]))))))

(defn cleanup-and-reset-turn
  "Give each team a new card, empty the clues and guesses, and advance to the
  next code master."
  ([state team]
   (-> state
       (assoc-in [team :card] (generate-card))
       (update-in [team :clues] empty)
       (update-in [team :guess] empty)
       (update-in [team :intercept-guess] empty)
       (advance-codemaster team)))
  ([state]
   (reduce cleanup-and-reset-turn state [:white :black])))

(defn provide-clues [state team player-index clues]
  (cond-> state
          (can-provide-clues? state team player-index)
          (assoc-in [team :clues] clues)))

(defn guess [state team player-index guess]
  (cond-> state
          (can-guess? state team player-index)
          (assoc-in [team :guess] guess)))

(defn intercept-guess [state team player-index intercept-guess]
  (cond-> state
          (can-intercept-guess? state team player-index)
          (assoc-in [team :intercept-guess] intercept-guess)))

(defn score-miscommunications [state team]
  (let [{:keys [guess card]} (state team)]
    (cond-> state
            (not= guess card)
            (update-in [team :miscommunications] (fnil inc 0)))))

(defn score-intercepts [state team]
  (let [other-team ({:white :black :black :white} team)
        {other-teams-card :card} (state other-team)
        {:keys [intercept-guess]} (state team)]
    (cond-> state
            (= other-teams-card intercept-guess)
            (update-in [team :intercepts] (fnil inc 0)))))
(defn store-previous-clues [state team]
  (let [{:keys [card clues]} (state team)
        cluemap (zipmap card clues)]
    (letfn [(store-clue [acc card-index]
              (let [clue (cluemap card-index "")]
                (update-in acc [team :words (dec card-index) :clues] conj clue)))]
      (reduce store-clue state [1 2 3 4]))))

(defn score-win [state team]
  (let [{:keys [intercepts]} (state team)]
    (assoc-in state [team :won?] (= 2 intercepts))))

(defn score-lost [state team]
  (let [{:keys [miscommunications]} (state team)]
    (assoc-in state [team :lost?] (= 2 miscommunications))))

(defn tied? [state]
  (let [{ww :won? lw :lost?} (state :white)
        {wb :won? lb :lost?} (state :black)]
    (or (and ww lw) (and wb lb) (and ww wb) (and lw lb))))

(defn resolve-turn
  ([state team]
   (cond-> state
           (can-resolve? state team)
           (-> (score-miscommunications team)
               (score-intercepts team)
               (store-previous-clues team)
               (score-win team)
               (score-lost team))))
  ([state]
   (reduce resolve-turn state [:white :black])))

(defn join-game [state team player-name]
  (let [white-team-players (map :name (get-in state [:white :players]))
        black-team-players (map :name (get-in state [:black :players]))
        all-player-names (set (into white-team-players black-team-players))]
    (cond-> state
            ((complement all-player-names) player-name)
            (update-in [team :players] conj {:name player-name}))))

(comment
  (cleanup-and-reset-turn state)

  (->> state
       (iterate #(advance-codemaster % :black))
       (take 5)
       (map #(get-in % [:black :codemaster-index])))

  (-> state
      (join-game :black "Sue")
      (join-game :black "Pat")
      (join-game :black "Sam")
      (join-game :black "Billy")
      (join-game :white "Mark")
      (join-game :white "Jo")
      (join-game :white "Mike")
      (join-game :white "Willy")
      ;; Willy is already on team white
      (join-game :black "Willy")
      cleanup-and-reset-turn
      (provide-clues :white 1 ["Clue A" "Clue B" "Clue C"])
      (provide-clues :black 1 ["Clue 1" "Clue 2" "Clue 3"])
      (guess :white 2 [1 2 3])
      (guess :black 2 [4 3 1])
      (intercept-guess :white 2 [2 3 1])
      (intercept-guess :black 2 [1 4 2])
      resolve-turn
      cleanup-and-reset-turn
      (provide-clues :white 2 ["RED" "GREEN" "BLUE"])
      (provide-clues :black 2 ["Dog" "Cat" "Pineapple"])
      (guess :white 1 [1 2 3])
      (guess :black 3 [4 3 1])
      (intercept-guess :white 3 [2 3 1])
      (intercept-guess :black 1 [1 4 2])
      resolve-turn
      cleanup-and-reset-turn
      )
  )

(def schema
  [:map
   [:players
    [:vector
     [:map
      [:name [:string {:min 1 :max 10}]]
      [:team [:enum :black :white]]]]]
   [:black
    [:map
     [:words [:vector {:min 4 :max 4} :string]]]]])

(mg/generate schema)

