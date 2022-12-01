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
  [:div#exampleModal.modal.fade
   {:tabindex "-1"}
   [:div.modal-dialog
    [:div.modal-content
     [:div.modal-header
      [:h1#exampleModalLabel.modal-title.fs-5 "Enter new value"]
      [:button.btn-close
       {:type            "button"
        :data-bs-dismiss "modal"}]]
     [:form
      [:div.modal-body
       [:div.form-group
        [:label.col-form-label {:for "recipient-name"} "New value:"]
        [:input#recipient-name.form-control
         {:type "text"
          :name "new-button-text"}]]]
      [:div.modal-footer
       [:button.btn.btn-secondary
        {:type            "button"
         :data-bs-dismiss "modal"}
        "Close"]
       [:button.btn.btn-primary
        {:type            "button"
         :data-bs-dismiss "modal"
         :hx-target       "#mainButton"
         :hx-post         "/modal/save"}
        "Save changes"]]]]]])

(defn chat-room [{:keys [params] :as _request}]
  (let [{:keys [roomname username]} params]
    (html5
      [:div {:hx-ext "ws" :ws-connect (format "/ws/%s/%s" roomname username)}
       [:div#roomList.sidebar
        {:style "width: 150px;"}]
       [:div#chat.container
        {:style "margin-left: 150px;"}
        [:h3#roomname roomname]
        [:button#mainButton.btn.btn-primary
         {:type           "button"
          :data-bs-toggle "modal"
          :data-bs-target "#exampleModal"}
         "Show modal"]
        room-create-modal
        [:div
         [:p {:id "notifications"}]
         [:form {:ws-send "true" :name "chat_message" :method :post}
          [:input.form-control {:name "chat_message" :autocomplete "off"}]]]]])))

