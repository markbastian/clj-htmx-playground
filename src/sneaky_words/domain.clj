(ns sneaky-words.domain
  (:require
    [clojure.java.io :as io]
    [clojure.pprint :as pp]
    [clojure.string :as str]))

;; https://www.fgbradleys.com/rules/rules6/Decrypto%20-%20rules.pdf
;; "https://raw.githubusercontent.com/WhoaWhoa/decrypto/master/wordlist.txt"

;; Generic utilities
(def conjv (comp vec (fnil conj [])))
(def conjs (comp set (fnil conj #{})))

;; Game-specific utilities
(defn ^:dynamic generate-card []
  (vec (take 3 (shuffle [1 2 3 4]))))

(defonce
  word-list
  (let [f (io/file "wordlist.txt")]
    (if (.exists f)
      (with-open [r (io/reader f)]
        (vec (line-seq r)))
      (let [f (slurp "https://raw.githubusercontent.com/WhoaWhoa/decrypto/master/wordlist.txt")]
        (spit "wordlist.txt" f)
        (str/split-lines f)))))

(defn ^:dynamic generate-words []
  (->> word-list shuffle (take 8)))

;; Predicates

(defn words-initialized? [state]
  (and
    (get-in state [:white :words])
    (get-in state [:black :words])))

(defn game-over?
  ([state team]
   (let [{:keys [won? lost?]} (state team)]
     (or won? lost? false)))
  ([state] (some (partial game-over? state) [:white :black])))

(defn can-resolve?
  ([state team]
   (every? (comp #{3} count (state team)) [:guess :card :clues :intercept-guess]))
  ([state] (every? (partial can-resolve? state) [:black :white])))

(defn can-provide-clues? [state team player-name]
  (let [{:keys [encryptor card clues]} (state team)]
    (and
      (= encryptor player-name)
      (= 3 (count card))
      (empty? clues))))

(defn can-guess? [state team player-name]
  (let [{:keys [players encryptor card guess]} (state team)]
    (and
      (players player-name)
      (not= encryptor player-name)
      (= 3 (count card))
      (empty? guess))))

(defn can-intercept-guess? [state team player-name]
  (let [{other-card :card} (state ({:white :black :black :white} team))
        {:keys [players intercept-guess]} (state team)]
    (and
      (players player-name)
      (= 3 (count other-card))
      (empty? intercept-guess))))

(defn tied? [state]
  (let [{ww :won? lw :lost?} (state :white)
        {wb :won? lb :lost?} (state :black)]
    (or (and ww lw) (and wb lb) (and ww wb) (and lw lb))))


;; Global actions taking the form of state = f(state). Functions of the form
;; state = f(state, team) are all supporting functions used by global action
;; functions.
(defn advance-encryptor [state team]
  (-> state
      (assoc-in [team :encryptor] (first (get-in state [team :player-order])))
      (update-in [team :player-order] #(vec (take (count %) (drop 1 (cycle %)))))))

(defn start-turn
  "Give each team a new card, empty the clues and guesses, and advance to the
  next encryptor."
  ([state team]
   (cond-> state
           (and
             (words-initialized? state)
             (not (game-over? state))
             (get-in state [team :words])
             (nil? (get-in state [team :card]))
             (nil? (get-in state [team :encryptor])))
           (-> (assoc-in [team :card] (generate-card))
               (advance-encryptor team))))
  ([state]
   (reduce start-turn state [:white :black])))

(defn start-game [state]
  (if-not (words-initialized? state)
    (let [[a b] (split-at 4 (generate-words))]
      (-> state
          (update :white merge {:words (mapv (fn [word] {:text word}) a)})
          (update :black merge {:words (mapv (fn [word] {:text word}) b)})
          start-turn))
    state))

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
      (-> (reduce store-clue state [1 2 3 4])
          (update team dissoc :card :clues :guess :intercept-guess :encryptor)))))

(defn score-win [state team]
  (let [{:keys [intercepts]} (state team)]
    (assoc-in state [team :won?] (= 2 intercepts))))

(defn score-lost [state team]
  (let [{:keys [miscommunications]} (state team)]
    (assoc-in state [team :lost?] (= 2 miscommunications))))

(defn score-and-cleanup-turn
  ([state team]
   (-> state
       (score-miscommunications team)
       (score-intercepts team)
       (store-previous-clues team)
       (score-win team)
       (score-lost team)))
  ([state]
   (if (and
         (can-resolve? state)
         (not (game-over? state)))
     (reduce score-and-cleanup-turn state [:white :black])
     state)))

(defn resolve-winner-if-any [state]
  (let [b (state :black)
        w (state :white)
        score-fn #(- (get % :intercepts 0) (get % :miscommunications 0))
        winner (cond
                 (tied? state) (let [black-score (score-fn b)
                                     white-score (score-fn w)]
                                 (cond
                                   (> black-score white-score) :black
                                   (> white-score black-score) :white
                                   :else :tied))
                 (or (b :won?) (w :lost?)) :black
                 (or (w :won?) (b :lost?)) :white)]
    (cond-> state winner (assoc :winning-team winner))))

(def resolve-turn (comp resolve-winner-if-any score-and-cleanup-turn))

;; Player action functions taking the form state = f(state, team, player, args)
;; Any functions in this block that have fewer arguments are utilities for
;; player action functions.

(defn determine-team-to-join [state]
  (let [nw (count (get-in state [:white :players]))
        nb (count (get-in state [:black :players]))]
    (cond
      (> nw nb) :black
      (> nb nw) :white
      :else (rand-nth [:white :black]))))

(defn join-game
  ([state team player-name]
   (let [players (into
                   (get-in state [:white :players] #{})
                   (get-in state [:black :players] #{}))]
     (if-not (or
               (game-over? state)
               (get players player-name))
       (let [team-to-join (or team (determine-team-to-join state))]
         (-> state
             (update-in [team-to-join :players] conjs player-name)
             (update-in [team-to-join :player-order] conjv player-name)))
       state)))
  ([state player-name]
   (join-game state (determine-team-to-join state) player-name)))

(defn provide-clues [state team player-name clues]
  (cond-> state
          (can-provide-clues? state team player-name)
          (assoc-in [team :clues] clues)))

(defn guess [state team player-name guess]
  (cond-> state
          (can-guess? state team player-name)
          (assoc-in [team :guess] guess)
          true
          (-> resolve-turn start-turn)))

(defn intercept-guess [state team player-name intercept-guess]
  (cond-> state
          (can-intercept-guess? state team player-name)
          (assoc-in [team :intercept-guess] intercept-guess)
          true
          (-> resolve-turn start-turn)))

(comment
  (with-redefs [generate-card (constantly [1 2 3])]
    (-> {}
        (join-game :black "Sue")
        (join-game :black "Pat")
        (join-game :black "Sam")
        (join-game :black "Billy")
        (join-game :white "Mark")
        (join-game :white "Jo")
        (join-game :white "Mike")
        (join-game :white "Willy")
        (join-game :black "Willy")
        (join-game :white "Willy")
        start-game
        (provide-clues :white "Mark" ["Clue A" "Clue B" "Clue C"])
        (provide-clues :black "Sue" ["Clue 1" "Clue 2" "Clue 3"])
        (guess :white "Mike" [1 2 3])
        (guess :black "Sam" [4 3 1])
        (join-game :black "John")
        (intercept-guess :white "Willy" [2 3 1])
        (intercept-guess :black "Pat" [1 4 2])
        (provide-clues :black "Pat" ["RED" "GREEN" "BLUE"])
        (provide-clues :white "Jo" ["Dog" "Cat" "Pineapple"])
        (guess :white "Mark" [1 2 3])
        (guess :black "Billy" [4 3 1])
        ;(intercept-guess :white "Jo" [2 3 1])
        ;(intercept-guess :black "Pat" [1 4 2])
        ;; Can't join when game is over
        ;(join-game :black "After Over")
        ))
  )



