(ns clj-htmx-playground.chat.pages
  (:require [clojure.java.io :as io]
            [hiccup.page :refer [html5 include-css include-js]]))

(def show-chat-login
  [:form.container
   [:div.form-group
    [:h3 "Simple Chat"]
    [:h2 "Join a room!"]
    [:label "Room"]
    [:input.form-control
     {:name         "roomname"
      :placeholder  "Enter room name"
      :autocomplete "off"}]
    [:label "Username"]
    [:input.form-control
     {:name         "username"
      :placeholder  "Enter username"
      :autocomplete "off"}]]
   [:button.btn.btn-primary
    {:type      "submit"
     :hx-post   "/chat"
     :hx-target "#app"}
    "Join"]])

(def landing-page
  (html5
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    [:style (slurp (io/resource "clj_htmx_playground/styles.css"))]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:div {:id "app"} show-chat-login]))

(def room-create-modal
  [:div#changeRoomModal.modal.fade
   {:tabindex "-1"}
   [:div.modal-dialog
    [:div.modal-content
     [:div.modal-header
      [:h1#changeRoomModalLabel.modal-title.fs-5 "Change Room"]
      [:button.btn-close
       {:type            "button"
        :data-bs-dismiss "modal"}]]
     [:form
      {:ws-send "true"
       :name    "change_room"
       :method  :post}
      [:div.modal-body
       [:div.form-group
        [:label.col-form-label "Destination room:"]
        [:input.form-control
         {:type         "text"
          :name         "room-name"
          :autocomplete "off"}]]]
      [:div.modal-footer
       [:button.btn.btn-secondary
        {:type            "button"
         :data-bs-dismiss "modal"}
        "Close"]
       [:button.btn.btn-primary
        {:type            "button"
         :data-bs-dismiss "modal"
         :ws-send         "true"
         :name            "change_room"
         :method          :post}
        "Go"]]]]]])

(defn chat-room [{:keys [params] :as _request}]
  (let [{:keys [roomname username]} params]
    (html5
      [:div {:hx-ext "ws" :ws-connect (format "/ws/%s/%s" roomname username)}
       [:div#roomList.sidebar
        {:style "width: 150px;"}]
       [:div#chat.container
        {:style "margin-left: 150px;"}
        [:p
         [:b "Current room:"]
         [:b [:i [:a#roomChangeLink.link-primary
                  {:data-bs-toggle "modal"
                   :data-bs-target "#changeRoomModal"}
                  roomname]]]]
        room-create-modal
        [:div
         [:p {:id "notifications"}]
         [:form {:ws-send "true" :name "chat_message" :method :post}
          [:input#chatPrompt.form-control {:name         "chat_message"
                                           :autocomplete "off"
                                           :onblur       "console.log('blur')"
                                           :onfocus      "console.log('focus')"
                                           :onchange     "console.log('change')"
                                           :onsubmit     "console.log('submit')"}]]]]])))

(def cards
  (html5
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    [:div
     [:p.fs-4 "Team White"]
     [:div.row
      [:div.col-sm-2
       [:div.card.text-center.text-bg-light.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 1"]
        [:div.card-body
         [:h5.card-title.text-center "Cowboy"]]]]
      [:div.col-sm-2
       [:div.card.text-center.text-bg-light.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 2"]
        [:div.card-body
         [:h5.card-title.text-center "Mexico"]]]]
      [:div.col-sm-2
       [:div.card.text-center.text-bg-light.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 3"]
        [:div.card-body
         [:h5.card-title.text-center "Starcraft"]]]]
      [:div.col-sm-2
       [:div.card.text-center.text-bg-light.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 4"]
        [:div.card-body
         [:h5.card-title.text-center "Horse"]]]]]
     [:div.input-group.mb-3
      [:span.input-group-text "4"]
      [:input.form-control {:type "text" :placeholder "Enter Clue"}]]
     [:div.input-group.mb-3
      [:span.input-group-text "2"]
      [:input.form-control {:type "text" :placeholder "Enter Clue"}]]
     [:div.input-group.mb-3
      [:span.input-group-text "1"]
      [:input.form-control {:type "text" :placeholder "Enter Clue"}]]]
    [:div
     [:p.fs-4 "Team Black"]
     [:div.row
      [:div.col-sm-2
       [:div.card.text-center.text-bg-secondary.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 1"]
        [:div.card-body
         [:h5.card-title.text-center "Banana"]]]]
      [:div.col-sm-2
       [:div.card.text-center.text-bg-secondary.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 2"]
        [:div.card-body
         [:h5.card-title.text-center "Clojure"]]]]
      [:div.col-sm-2
       [:div.card.text-center.text-bg-secondary.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 3"]
        [:div.card-body
         [:h5.card-title.text-center "Monkey"]]]]
      [:div.col-sm-2
       [:div.card.text-center.text-bg-secondary.mb-3 {:style "max-width: 18rem;"}
        [:div.card-header "Word 4"]
        [:div.card-body
         [:h5.card-title.text-center "Hacker"]]]]]]))