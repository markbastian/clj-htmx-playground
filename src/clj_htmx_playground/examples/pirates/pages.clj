(ns clj-htmx-playground.examples.pirates.pages
  (:require
    [clojure.data.json :as json]
    [clojure.string :as str]
    [hiccup.page :refer [html5 include-css include-js]]))


;;https://developer.mozilla.org/en-US/docs/Web/CSS/transform-function/rotate

(def tile-dim 8)

(defn image [id image & {:keys [x y transition]}]
  (let [tx       (some->> x (format "translateX(%spx)"))
        ty       (some->> y (format "translateY(%spx)"))
        trns     (some->> transition (format "transition: %s;"))
        tx-style (when (or tx ty)
                   (cond->
                     (format
                       "transform: %s;"
                       (str/join " " (filter identity [tx ty])))
                     trns
                     (str trns)))]
    [:image
     (cond->
       {:id      id
        :href    image
        :height  tile-dim
        :width   tile-dim
        :name    id
        :hx-vals (json/write-str {:id id :image image})
        :hx-post "/svg/canvas/click"
        :hx-swap "outerHTML"}
       (or tx ty)
       (assoc :style tx-style))]))

(def symbols
  (conj
    (->> [:bottle :hat :sword :keys :pistol :flag]
         (repeat 5)
         (mapcat shuffle)
         (into [:start]))
    :end))

(defn symbol->color [symbol]
  (case symbol
    :start "magenta"
    :bottle "red"
    :hat "yellow"
    :sword "green"
    :flag "blue"
    :keys "orange"
    :pistol "cyan"
    :end "brown"))

(def track
  [[0 0]
   [0 1] [1 1] [2 1] [3 1] [4 1] [5 1]
   [5 2]
   [5 3]
   [0 3] [1 3] [2 3] [3 3] [4 3] [5 3]
   [0 4]
   [0 5]
   [0 6] [1 6] [2 6] [3 6] [4 6] [5 6]
   [5 7]
   [5 8]
   [0 9] [1 9] [2 9] [3 9] [4 9] [5 9]
   [0 10]])

(count track)

(def board
  {:tiles {}})

(def cards
  [{:loc [0 11.5] :symbol :bottle}
   {:loc [0.5 11.5] :symbol :hat}
   {:loc [1 11.5] :symbol :sword}
   {:loc [1.5 11.5] :symbol :flag}
   {:loc [2 11.5] :symbol :keys}
   {:loc [2.5 11.5] :symbol :pistol}])

(def canvas
  [:svg {:width "100%" :height "100%" :viewBox "0 0 200 200"
         :xmlns "http://www.w3.org/2000/svg"
         :style "background-color:black"}
   (for [i (range (count track))
         :let [[x y] (track i)
               symbol (symbols i)]]
     [:rect
      {:id     (format "space-%s" i)
       :x      (* tile-dim x)
       :y      (* tile-dim y)
       :width  tile-dim
       :height tile-dim
       :stroke (symbol->color symbol)}])
   (for [i (range (count cards))
         :let [{:keys [symbol] [x y] :loc} (cards i)]]
     [:rect
      {:x      (* tile-dim x)
       :y      (* tile-dim y)
       :width  tile-dim
       :height tile-dim
       :fill   (symbol->color symbol)}])
   (image "mypirate1" "/pirate1.png")
   (image "mypirate2" "/pirate2.png" {:x tile-dim})
   (image "mypirate3" "/pirate3.png" {:x (* 2 tile-dim)})
   (image "mypirate4" "/pirate4.png" {:x (* 3 tile-dim)})])

(defn main-page []
  [:div {:hx-ext     "ws"
         :ws-connect (format "/svg/ws/%s" "abc")}
   canvas])

(defn wrap-as-page [content]
  (html5
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    content))