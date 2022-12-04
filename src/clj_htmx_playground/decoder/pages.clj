(ns clj-htmx-playground.decoder.pages
  (:require [hiccup.page :refer [html5 include-css include-js]]))

(def cards
  (html5
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    [:div.container
     [:p.fs-4 "Team White"]
     [:div.row
      [:div.col
       [:div.card.text-center.text-bg-light.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 1"]
        [:div.card-body
         [:h5.card-title.text-center "Cowboy"]]]]
      [:div.col
       [:div.card.text-center.text-bg-light.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 2"]
        [:div.card-body
         [:h5.card-title.text-center "Mexico"]]]]
      [:div.col
       [:div.card.text-center.text-bg-light.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 3"]
        [:div.card-body
         [:h5.card-title.text-center "Starcraft"]]]]
      [:div.col
       [:div.card.text-center.text-bg-light.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 4"]
        [:div.card-body
         [:h5.card-title.text-center "Horse"]]]]]
     ;; TODO - I can templatize this for the clue giver and have another
     ;; template for the clue receivers.
     [:form.form-group
      [:div.input-group.mb-3
       [:span.input-group-text.text-bg-light "4"]
       [:input#white-clue-1.form-control.text-bg-light
        {:type "text" :name "white-clue-1" :placeholder "Enter Clue" :autocomplete "off"}]]
      [:div.input-group.mb-3
       [:span.input-group-text.text-bg-light "2"]
       [:input#white-clue-2.form-control.text-bg-light
        {:type "text" :name "white-clue-2" :placeholder "Enter Clue" :autocomplete "off"}]]
      [:div.input-group.mb-3
       [:span.input-group-text.text-bg-light "1"]
       [:input#white-clue-3.form-control.text-bg-light
        {:type "text" :name "white-clue-3" :placeholder "Enter Clue" :autocomplete "off"}]]
      [:button.btn.btn-light {:type "button" :hx-post "/submitClues"} "Submit Clues"]]]
    [:div.container
     [:p.fs-4 "Team Black"]
     [:div.row
      [:div.col
       [:div.card.text-center.text-bg-secondary.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 1"]
        [:div.card-body
         [:h5.card-title.text-center "Banana"]]]]
      [:div.col
       [:div.card.text-center.text-bg-secondary.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 2"]
        [:div.card-body
         [:h5.card-title.text-center "Clojure"]]]]
      [:div.col
       [:div.card.text-center.text-bg-secondary.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 3"]
        [:div.card-body
         [:h5.card-title.text-center "Monkey"]]]]
      [:div.col
       [:div.card.text-center.text-bg-secondary.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 4"]
        [:div.card-body
         [:h5.card-title.text-center "Hacker"]]]]]
     [:form.form-group
      [:div.input-group.mb-3
       [:span.input-group-text.text-bg-secondary "4"]
       [:input#black-clue-1.form-control.text-bg-secondary
        {:name "black-clue-1" :placeholder "Enter Clue" :autocomplete "off"}]]
      [:div.input-group.mb-3
       [:span.input-group-text.text-bg-secondary "2"]
       [:input#black-clue-2.form-control.text-bg-secondary
        {:name "black-clue-2" :placeholder-primary "Enter Clue" :autocomplete "off"}]]
      [:div.input-group.mb-3
       [:span.input-group-text.text-bg-secondary "1"]
       [:input#black-clue-3.form-control.text-bg-secondary
        {:name "black-clue-3" :placeholder "Enter Clue" :autocomplete "off"}]]
      [:button.btn.btn-secondary {:type "button" :hx-post "/submitClues"} "Submit Clues"]]]))