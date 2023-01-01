(ns clj-htmx-playground.chat.pages
  (:require
    [clj-htmx-playground.utils :as u]
    [clojure.java.io :as io]
    [hiccup.page :refer [html5 include-css include-js]]))

(defn wrap-as-page [content]
  (html5
    (include-css
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css"
      "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.2.1/css/all.min.css")
    (include-js
      "https://unpkg.com/htmx.org@1.8.4"
      "https://unpkg.com/htmx.org/dist/ext/ws.js"
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
    [:style (slurp (io/resource "clj_htmx_playground/styles.css"))]
    [:style (slurp (io/resource "clj_htmx_playground/sidebars.css"))]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    content))

(defn show-chat-login [& attributes]
  [:div (into {:id "app"} attributes)
   [:form.container
    [:div.form-group
     [:h4.text-center "Welcome to Markchat!"]
     ;; Can probably inline an error band here and then oob swap it in
     ;; just need to not show the band if no error
     [:input.form-control
      {:name         "username"
       :placeholder  "Enter username"
       :autocomplete "off"}]]
    [:div.d-grid.gap-2
     [:button.btn.btn-primary.btn-dark
      {:type      "submit"
       :hx-post   "/chat/room"
       :hx-target "#app"
       :hx-vals   (u/to-json-str {:room-name "public"})}
      "Join"]]]])

(def landing-page
  (wrap-as-page (show-chat-login)))

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
  [:li [:a.link-dark.rounded.text-white
        (into {:id label :href "#"} attributes)
        label]])

;; TODO - should probably put "command" as key in hx-vals rather
;; than relying on name as a [:HEADERS :HX-Trigger-Name] param.
(defn occupied-rooms-list [rooms-names]
  (let [attrs {:ws-send "true" :name "change-room" :method :post}
        f (fn [room-name]
            (sidebar-sublist-item
              room-name
              (assoc attrs :hx-vals (u/to-json-str {:room-name room-name}))))]
    (->> rooms-names sort (map f))))

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

(defn navbar-sublist [& r]
  (into [:ul.dropdown-menu] r))
(defn navbar-list [list-name id]
  ; Note quite there yet
  [:li.nav-item.dropdown
   [:a.nav-link.dropdown-toggle
    {:href           "#"
     :role           "button"
     :data-bs-toggle "dropdown"}
    list-name]
   (navbar-sublist {:id id})]

  (let [collapse-id (format "%s-collapse" id)]
    [:li.mb-1.text-white
     [:button.btn.btn-toggle.align-items-center.rounded.collapsed.text-white
      {:data-bs-toggle "collapse"
       :data-bs-target (format "#%s" collapse-id)}
      list-name]
     [:div.collapse
      {:id collapse-id}
      (sidebar-sublist {:id id})]]))

(defn navbar []
  [:nav.navbar.navbar-expand-lg.navbar-dark.bg-dark
   [:div.container-fluid
    [:button.navbar-toggler
     {:type           "button"
      :data-bs-toggle "collapse"
      :data-bs-target "#navbarToggler"}
     [:span.navbar-toggler-icon]]
    [:div#navbarToggler.collapse.navbar-collapse
     [:ul.navbar-nav.me-auto.mb-2.mb-lg-0
      (navbar-list "Rooms" "roomList")
      (navbar-list "Users" "userList")]]]])

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
  [:div#chat.overflow-scroll
   {:style "height: calc(100vh - 100px); overflow: auto; flex-grow: 1;"}
   [:p.p-2.border
    [:b "You are in "]
    (room-change-link room-name)]
   room-create-modal
   [:div.p-2
    (notifications-pane)]])

(defn chat-prompt
  [room-name & attributes]
  [:div#chatPrompt.input-group.mb-3
   [:input.form-control
    (into
      {:name         "chat-message"
       :placeholder  (format "Message #%s" room-name)
       :autocomplete "off"}
      attributes)]
   [:button.btn.btn-outline-secondary.fixed-bottom
    {:type    "button"
     :ws-send "true"
     :name    "chat-message"
     :method  :post}
    [:i.fa.fa-paper-plane]]])

(defn chat-page [{:keys [params] :as _request}]
  (let [{:keys [room-name username]} params]
    (html5
      [:div.mb-3.vh-100
       {:hx-ext     "ws"
        :ws-connect (format "/chat/ws/%s/%s" room-name username)}
       (navbar)
       (chat-pane room-name)
       [:form
        {:ws-send "true"
         :name    "chat-message"
         :method  :post}
        (chat-prompt room-name)]])))