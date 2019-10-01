(ns vfprocess.schema
  "Contains custom resolver and a function to provide the full schema"
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [next.jdbc.sql :as sql]
            [com.walmartlabs.lacinia.schema :as schema]

            [vfprocess.db.queries :refer [db
                                          query
                                          get-process-inputs
                                          get-process-outputs
                                          query-economic-resource
                                          query-economic-event
                                          query-all-economic-event
                                          query-all-economic-resource
                                          get-agent-inventoriedEconomicResource
                                          get-agent-economic-event]]
            [clojure.edn :as edn]))

(defn resolver-map
  []
  {:query/process (fn [_ args _] (let [{:keys [id]} args]
                                   (query :Process id)))
   :query/get-inputs (fn [_ _ value] (let [{:keys [id]} value]
                                       (get-process-inputs id)))
   :query/get-outputs (fn [_ _ value] (let [{:keys [id]} value]
                                        (get-process-outputs id)))
   :query/get-agent-economic-event (fn [_ _ value] (let [{:keys [id]} value]
                                                     (get-agent-economic-event id)))

   :query/get-agent-inventoriedEconomicResource (fn [_ _ value] (let [{:keys [id]} value]
                                                                  (get-agent-inventoriedEconomicResource id)))
   :query/allProcesses (fn [_ _ _] (query :Process))
   :query/agent (fn [_ args _] (let [{:keys [id]} args]
                                 (query :Agent id)))
   :query/allAgents (fn [_ _ _] (query :Agent))
   :query/economicEvent (fn [_ args _] (let [{:keys [id]} args]
                                         (query-economic-event id)))
   :query/allEconomicEvents (fn [_ _ _] (query-all-economic-event))
   :query/economicResource (fn [_ args _] (let [{:keys [id]} args]
                                            (query-economic-resource id)))
   :query/allEconomicResources (fn [_ _ _] (query-all-economic-resource))
   :query/resourceSpecification (fn [_ args _] (let [{:keys [id]} args]
                                                 (query :ResourceSpecification id)))
   :query/allResourceSpecification (fn [_ _ _] (query :ResourceSpecification))
   :query/action (fn [_ args _] (let [{:keys [id]} args]
                                  (query :Action id)))
   :query/allActions (fn [_ _ _] (query :Action))
   :query/unit (fn [_ args _] (let [{:keys [id]} args]
                                (query :Unit id)))
   :query/allUnits (fn [_ _ _] (query :Unit))
  ;  :mutation/createEconomicEvent (fn [_ args _] (println args))
   })

  (defn load-schema
    []
    (-> (io/resource "process-schema.edn")
        slurp
        edn/read-string
        (util/attach-resolvers (resolver-map))
        schema/compile))
