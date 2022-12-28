(ns clj-htmx-playground.decoder.commands-old
  (:require
    [clojure.pprint :as pp]
    [clojure.tools.logging :as log]))

;;https://www.fgbradleys.com/rules/rules6/Decrypto%20-%20rules.pdf

;; There are really only a few commands
;; - join game
;; -

(def conjv (fnil conj []))


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
  (-> state
      (assoc-in [team :codemaster] (first (get-in state [team :player-order])))
      (update-in [team :player-order] #(vec (take (count %) (drop 1 (cycle %)))))))


(defn cleanup-and-reset-turn
  "Give each team a new card, empty the clues and guesses, and advance to the
  next code master."
  ([state team]
   (-> state
       (assoc-in [team :card] (generate-card))
       ;(dissoc team :clues :guess :guess)
       (advance-codemaster team)))
  ([state]
   (reduce cleanup-and-reset-turn state [:white :black])))

(defn determine-team-to-join [state]
  (let [nw (count (get-in state [:white :players]))
        nb (count (get-in state [:black :players]))]
    (cond
      (> nw nb) :black
      (> nb nw) :white
      :else (rand-nth [:white :black]))))

(defn log-success [context command & effects]
  (-> context
      (update :event-log conjv (assoc command :success true))
      (assoc :effects (vec effects))))

(defn log-failure
  ([context command effects]
   (-> context
       (update :event-log conjv (assoc command :success false))
       (assoc :effects effects)))
  ([context command]
   (update context :event-log conjv (assoc command :success false))))

(derive ::awaiting-players ::game-state)
(derive ::awaiting-clues ::game-state)
(derive ::awaiting-guesses ::game-state)
(derive ::resolve-guesses ::game-state)

(defn dispatch-fn [context {:keys [command]}]
  [(get-in context [:state :game-state]) command])

(defmulti validate-command dispatch-fn)
(defmulti handle-command dispatch-fn)
(defmulti state-transition dispatch-fn)

(defmethod validate-command :default [context command]
  (println (format "#################### Unhanded validation: %s ####################"
                   (dispatch-fn context command))))

(defmethod validate-command [::awaiting-players :begin-game] [{:keys [state]} _command]
  (let [white-team-players (get-in state [:white :players])
        black-team-players (get-in state [:black :players])
        problems (cond-> []
                         (< (count white-team-players) 2)
                         (conj [:invalid-team-action "White team needs at least 2 players."])
                         (< (count black-team-players) 2)
                         (conj [:invalid-team-action "Black team needs at least 2 players."]))]
    (when (seq problems) problems)))

(defmethod validate-command [::game-state :begin-game] [context _]
  (let [game-state (get-in context [:state :game-state])]
    (when (and game-state (not= :awaiting-players game-state))
      [[:invalid-action "Game has already started"]])))

(defmethod handle-command :default [context {:keys [command] :as cmd}]
  (format "#################### Unhanded command: %s ####################"
          (dispatch-fn context command)))

(defmethod validate-command [::game-state :join-game] [{:keys [state]} {:keys [player-name]}]
  (let [white-team-players (get-in state [:white :players])
        black-team-players (get-in state [:black :players])
        all-player-names (merge {} white-team-players black-team-players)]
    (when (all-player-names player-name)
      [[:invalid-player-action player-name (format "The player name '%s' is already taken." player-name)]])))

(defmethod handle-command [::game-state :join-game] [{:keys [state] :as context}
                                                     {:keys [team player-name]}]
  (let [team-to-join (or team (determine-team-to-join state))]
    (-> context
        (assoc-in [:state team-to-join :players player-name] {})
        (update-in [:state team-to-join :player-order] conjv player-name))))

(defmethod handle-command [::game-state :begin-game] [context _command]
  (update context :state cleanup-and-reset-turn))

(defmethod state-transition :default [context command]
  (println (format "#################### Unhanded state transition: %s ####################"
                   (dispatch-fn context command))))

(defmethod state-transition [::game-state :join-game] [context _command]
  context)

(defmethod state-transition [::awaiting-players :begin-game] [context _command]
  (assoc-in context [:state :game-state] ::awaiting-clues))

(defmethod state-transition [::awaiting-clues :provide-clues] [context command]
  (cond->
    context
    (and
      (seq (get-in context [:state :white :clues]))
      (seq (get-in context [:state :black :clues])))
    (assoc-in [:state :game-state] ::awaiting-guesses)))

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
                (update-in acc [team :words (dec card-index) :clues] conjv clue)))]
      (reduce store-clue state [1 2 3 4]))))

(defn score-win [state team]
  (let [{:keys [intercepts]} (state team)]
    (assoc-in state [team :won?] (= 2 intercepts))))

(defn score-lost [state team]
  (let [{:keys [miscommunications]} (state team)]
    (assoc-in state [team :lost?] (= 2 miscommunications))))

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

(defmethod state-transition [::awaiting-guesses :provide-guess] [context command]
  (cond->
    context
    (and
      (seq (get-in context [:state :white :guess]))
      (seq (get-in context [:state :black :guess]))
      (seq (get-in context [:state :white :intercept-guess]))
      (seq (get-in context [:state :black :intercept-guess])))
    ;; This is not a game state. This is an effect.
    ;; We should actually resolve the effect here automatically I think
    ;; Or maybe the effects dispatcher handles it?
    (update :state (fn [state]
                     (println "RESOLVING TURN")
                     (resolve-turn state)))
    ;(assoc-in [:state :game-state] ::resolve-guesses)
    ))

(defmethod validate-command [::awaiting-clues :provide-clues] [context {:keys [team player-name clues]}]
  (let [{:keys [codemaster players] existing-clues :clues} (get-in context [:state team])]
    (cond
      (nil? (players player-name))
      [:invalid-player-action player-name (format "%s is not on team %s." player-name (name team))]
      (not= codemaster player-name)
      [:invalid-player-action player-name (format "%s is not the codemaster." player-name)]
      (seq existing-clues)
      [:invalid-player-action player-name "Clues have already been set."]
      (not= 3 (count (map seq clues)))
      [:invalid-player-action player-name "You must provide 3 clues"])))

(defmethod validate-command [::awaiting-guesses :provide-guess] [context {:keys [team player-name guess]}]
  (let [{:keys [codemaster players]} (get-in context [:state team])]
    (cond
      (nil? (players player-name))
      [:invalid-player-action player-name (format "%s is not on team %s." player-name (name team))]
      (= codemaster player-name)
      [:invalid-player-action player-name "The codemaster may not provide the guess."]
      (not= 3 (count guess))
      [:invalid-player-action player-name "You must provide 3 clues"])))

(defmethod handle-command [::awaiting-clues :provide-clues] [context {:keys [team clues]}]
  (assoc-in context [:state team :clues] clues))

(defmethod handle-command [::awaiting-guesses :provide-guess] [context {:keys [team guess]}]
  (assoc-in context [:state team :guess] guess))

;; What I really want is a generic handler....
(defn handle [context command]
  (if-some [error (validate-command context command)]
    (log-failure context command error)
    (-> context
        (handle-command command)
        (log-success command)
        (state-transition command))))

(comment
  (remove-ns 'clj-htmx-playground.decoder.commands-old)
  (->> [{:command     :join-game
         :team        :white
         :player-name "Bobby"}
        {:command     :join-game
         :team        :black
         :player-name "Kim"}
        {:command :begin-game}
        {:command :join-game :player-name "Pat" :team :black}
        {:command :join-game :player-name "Sam" :team :white}
        {:command :join-game :player-name "Sam"}
        {:command :begin-game}
        {:command :join-game :player-name "Mark"}
        {:command :begin-game}
        {:command     :provide-clues
         :team        :white
         :player-name "Bobby"
         :clues       ["A" "B" "C"]}
        {:command     :provide-clues
         :team        :white
         :player-name "Bobby"
         :clues       ["A" "B" "D"]}
        {:command     :provide-clues
         :team        :black
         :player-name "Kim"
         :clues       ["A" "B" "C"]}
        {:command     :provide-guess
         :team        :black
         :player-name "Kim"
         :guess       [1 2 3]}
        {:command     :provide-guess
         :team        :black
         :player-name "Bobby"
         :guess       [1 2 3]}
        {:command     :provide-guess
         :team        :black
         :player-name "Pat"
         :guess       [1 2 3]}
        #_{:command     :provide-intercept-guess
         :team        :black
         :player-name "Pat"
         :guess       [1 2 3]}
        ]
       (reduce handle {:state {:game-state ::awaiting-players}})
       :state))

(defn guess [state team player-index guess]
  (cond-> state
          (can-guess? state team player-index)
          (assoc-in [team :guess] guess)))

(defn intercept-guess [state team player-index intercept-guess]
  (cond-> state
          (can-intercept-guess? state team player-index)
          (assoc-in [team :intercept-guess] intercept-guess)))



;(derive ::rectangle ::shape)
;(derive ::square ::rectangle)
;(derive ::circle ::shape)
;
;(defmulti print-shape (fn [{:keys [type]} {:keys [command]}] [type command]))
;
;(defmethod print-shape [::shape :draw] [{:keys [type]} {:keys [command]}]
;  (println "I am a shape"))
;
;(defmethod print-shape [::rectangle :draw] [{:keys [type]} {:keys [command]}]
;  (println "I am a rectangle"))
;
;(print-shape {:type ::square} {:command :draw})
;(print-shape {:type ::circle} {:command :draw})
