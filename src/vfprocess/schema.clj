(ns vfprocess.schema
  "Contains custom resolver and a function to provide the full schema"
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [next.jdbc.sql :as sql]
            [com.walmartlabs.lacinia.schema :as schema]

            [vfprocess.db.traversal :refer [incoming-vf-dfs
                                            first-neighbors]]
            [vfprocess.db.queries :refer [db
                                          query
                                          query-process
                                          create-economic-event]]
            [clojure.edn :as edn]))

(defn find-economicResource-by-id [id]
  (let [economicResource (query "EconomicResource" id)
        first-node {:type (str "economicResource_" id)}
        incoming-valueflows (incoming-vf-dfs first-node)]
    (-> economicResource
        (merge {:track incoming-valueflows}))))

;; TODO: from scratch
(defn mutationNewEconomicEvent
  [args]
  (let [{:keys [event]} args
        economicResource (query "EconomicResource" (:resourceInventoriedAs event))
        toEconomicResource (query "EconomicResource" (:toResourceInventoriedAs event))
        action (query "Action" (:action event))
        economicEvent (create-economic-event event)
        ]
    (cond
      (some? (:resourceInventoriedAs event))
      (cond
        (= (:resourceEffect action) "+")
        (if (= (:createResource event) true)
          (sql/insert! db :EconomicResource economicResource)
          (do
            (sql/update! db :EconomicResource
                         {:accountingQuantityNumericValue (+
                                                           (:accountingQuantityNumericValue economicResource)
                                                           (:resourceQuantityNumericValue event))
                          :onhandQuantitynumericValue (+
                                                       (:accountingQuantityNumericValue economicResource)
                                                       (:resourceQuantityNumericValue event))}
                         {:id (:id economicResource)})
            event))
        
        (= (:resourceEffect action) "-")
        (do
          (sql/update! db :EconomicResource
                       {:accountingQuantityNumericValue (-
                                                         (:accountingQuantityNumericValue economicResource)
                                                         (:resourceQuantityNumericValue event))
                        :onhandQuantitynumericValue (-
                                                     (:accountingQuantityNumericValue economicResource)
                                                     (:resourceQuantityNumericValue event))}
                       {:id (:id economicResource)})
          event)
        (or (= (:label action) "transferCustody") (= (:label action) "transferComplete") (= (:label action) "move"))
        (do
          (sql/update! db :EconomicResource
                       {:onhandQuantitynumericValue (-
                                                     (:accountingQuantityNumericValue economicResource)
                                                     (:resourceQuantityNumericValue event))}
                       {:id (:id economicResource)})
          event)
        (or (= (:label action) "transferAllRights") (= (:label action) "transferComplete") (= (:label action) "move"))
        (do
          (sql/update! db :EconomicResource
                       {:accountingQuantityNumericValue (-
                                                         (:accountingQuantityNumericValue economicResource)
                                                         (:resourceQuantityNumericValue event))}
                       {:id (:id economicResource)})
          event)
        :else nil)
      (some? (:toResourceInventoriedAs event))
      (do
        (sql/update! db :EconomicResource
                     {:accountingQuantityNumericValue (+
                                                       (:accountingQuantityNumericValue toEconomicResource)
                                                       (:resourceQuantityNumericValue event))
                      :onhandQuantitynumericValue (+
                                                   (:accountingQuantityNumericValue toEconomicResource)
                                                   (:resourceQuantityNumericValue event))}
                     {:id (:id economicResource)})
        event)
      :else nil
      )
    )
)

(defn resolver-map
  []
  {:query/process (fn [_ args _] (let [{:keys [id]} args]
                                             (query-process id)))
   :query/allProcesses (fn [_ _ _] (query-process))
   :query/agent (fn [_ args _] (let [{:keys [id]} args]
                                           (query "Agent" id)))
   :query/allAgents (fn [_ _ _] (query "Agent"))
   :query/economicEvent (fn [_ args _] (let [{:keys [id]} args]
                                                   (query "EconomicEvent" id)))
   :query/allEconomicEvents (fn [_ _ _] (query "EconomicEvent"))
   :query/economicResource (fn [_ args _] (let [{:keys [id]} args]
                                                      (find-economicResource-by-id id)
                                                      ))
   :query/allEconomicResources (fn [_ _ _] (query "EconomicResource"))
   :query/resourceSpecification (fn [_ args _] (let [{:keys [id]} args]
                                                           (query "ResourceSpecification" id)
                                                           ))
   :query/allResourceSpecification (fn [_ _ _] (query "ResourceSpecification"))
   :query/action (fn [_ args _] (let [{:keys [id]} args]
                                            (query "Action" id)))
   :query/allActions (fn [_ _ _] (query "Action"))
   :query/unit (fn [_ args _] (let [{:keys [id]} args]
                                          (query "Unit" id)))
   :query/allUnits (fn [_ _ _] (query "Unit"))
   :mutation/createEconomicEvent (fn [_ args _] (mutationNewEconomicEvent args))
   })

       (defn load-schema
       []
       (-> (io/resource "process-schema.edn")
           slurp
           edn/read-string
           (util/attach-resolvers (resolver-map))
           schema/compile))
