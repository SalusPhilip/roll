(ns roll.sente
  (:require [taoensso.timbre :refer [info]]
            [taoensso.sente  :as sente :refer [cb-success?]]
            [taoensso.sente.packers.transit :as st]
            [roll.util :as u]))




(defmulti event-msg-handler :id)

; Default/fallback case (no other matching handler)
(defmethod event-msg-handler
  :default
  [{:as ev-msg :keys [event id]}])



(def sente-fns (atom nil))

(defn send-msg [& args]
  (some-> (:chsk-send! @sente-fns)
          (apply args)))



(defn get-packer [& [{:keys [write-handlers read-handlers]}]]
  (st/->TransitPacker
   :json
   {:handlers (into {} (apply merge write-handlers))}
   {:handlers (into {} (apply merge read-handlers))}))




(defn init-sente [& [{:as opts :keys [packer]}]]
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket-client!
         "/chsk"
         {:packer (get-packer packer)})]
    {:chsk       chsk
     :ch-chsk    ch-recv
     :chsk-send! send-fn
     :chsk-state state}))




(defonce router_ (atom nil))

(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! [& [{:as opts :keys [handler]}]]
  (stop-router!)
  (let [{:keys [ch-chsk]} (->> (dissoc opts :handler)
                               (init-sente)
                               (reset! sente-fns))]
    
    (->> (or handler event-msg-handler)
         (sente/start-client-chsk-router! ch-chsk)
         (reset! router_))))


(def start! start-router!)
(def stop!  stop-router!)





(comment
  
  {:write-handlers 
   (merge
    (u/resolve-cljs 'linked.transit     'write-handlers)
    (u/resolve-cljs 'datascript.transit 'write-handlers))
         
   :read-handlers
   (merge 
    (u/resolve-cljs 'linked.transit     'read-handlers)
    (u/resolve-cljs 'datascript.transit 'read-handlers))})
