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
     {:name         "room-name"
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

(defn wrap-as-page [content]
  (html5
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    [:style (slurp (io/resource "clj_htmx_playground/styles.css"))]
    [:style (slurp (io/resource "clj_htmx_playground/sidebars.css"))]
    content))

(def landing-page
  (html5
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    [:style (slurp (io/resource "clj_htmx_playground/styles.css"))]
    [:style (slurp (io/resource "clj_htmx_playground/sidebars.css"))]
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
       :name    "change-room"
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
         :name            "change-room"
         :method          :post}
        "Go"]]]]]])

(defn sidebar-sublist-item [label & attributes]
  [:li [:a.link-dark.rounded
        (into {:id label :href "#"} attributes)
        label]])

(defn sidebar-sublist [& r]
  (into [:ul.btn-toggle-nav.list-unstyled.fw-normal.pb-1.small] r))

(defn sidebar-list [list-name id]
  (let [collapse-id (format "%s-collapse" id)]
    [:li.mb-1
     [:button.btn.btn-toggle.align-items-center.rounded.collapsed
      {:data-bs-toggle "collapse"
       :data-bs-target (format "#%s" collapse-id)}
      list-name]
     [:div.collapse
      {:id collapse-id}
      (sidebar-sublist {:id id})]]))

(defn sidebar []
  [:ul.list-unstyled.ps-0
   (sidebar-list "Rooms" "roomList")
   (sidebar-list "Users" "userList")])

(defn room-change-link [room-name & attributes]
  [:a#roomChangeLink.link-primary
   (into
     {:data-bs-toggle "modal"
      :data-bs-target "#changeRoomModal"}
     attributes)
   room-name])

(defn notifications-pane [& r]
  (into [:div#notifications] r))

(defn chat-pane [room-name]
  [:div#chat.h-100
   [:p.p-2.border
    [:b "You are in room "]
    (room-change-link room-name)]
   room-create-modal
   [:div.p-2
    (notifications-pane)]])

(defn chat-prompt
  [room-name & attributes]
  [:input#chatPrompt.form-control
   (into
     {:name         "chat-message"
      :placeholder  (format "Message #%s" room-name)
      :autocomplete "off"}
     attributes)])

(defn chat-page [{:keys [params] :as _request}]
  (let [{:keys [room-name username]} params]
    (html5
      [:div.row.border.h-100
       {:hx-ext     "ws"
        :ws-connect (format "/ws/%s/%s" room-name username)}
       [:div.p-2.col-xs-10.col-sm-2 (sidebar)]
       [:div.p-2.col-xs-10.col-sm-7 (chat-pane room-name)]
       [:form.fixed-bottom
        {:ws-send "true" :name "chat-message" :method :post}
        (chat-prompt room-name)]])))
