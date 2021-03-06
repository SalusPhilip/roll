(ns roll.file
  (:require [taoensso.timbre :refer [info]]
            [integrant.core  :as ig]
            [roll.filewatch :as fw]
            [roll.util :as u :refer [resolve-map-syms]]))



(defmethod ig/init-key :roll/file [_ {:keys [path init watch] :as opts}]
  (info "starting roll/file:")
  (info (u/spp opts))
  
  (let [{:as opts :keys [init watch]} (resolve-map-syms opts)]
    
    (when init
      (init path))

    (when-let [watch (if (true? watch) init watch)]
      (fw/start-watch! path path watch)
      ;; return stop-fn
      #(fw/stop-watch! path))))



(defmethod ig/halt-key! :roll/file [_ stop-fn]
  (when stop-fn
    (info "stopping roll/file...")
    (stop-fn)))

