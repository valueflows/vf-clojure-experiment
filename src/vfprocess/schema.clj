(ns vfprocess.schema
  "Contains custom resolver and a function to provide the full schema"
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [next.jdbc.sql :as sql]
            [com.walmartlabs.lacinia.schema :as schema]

            [vfprocess.db.traversal :refer [incoming-vf-dfs
                                            first-neighbors]]
            [vfprocess.db.queries :refer [db
                                          queryProcesses
                                          queryAgents
                                          queryEconomicResources
                                          queryEconomicEvents
                                          queryResourceSpecifications
                                          queryUnits
                                          queryActions
                                          createEconomicEvent]]
            [clojure.edn :as edn]))

(defn find-economicResource-by-id [id]
  (let [economicResource (queryEconomicResources id)
        first-node {:type (str "economicResource_" id)}
        incoming-valueflows (incoming-vf-dfs first-node)]
    (-> economicResource
        (merge {:track incoming-valueflows}))))

;; TODO: from scratch
(defn mutationNewEconomicEvent
  [args]
  (let [{:keys [event]} args
        economicResource (queryEconomicResources (:resourceInventoriedAs event))
        toEconomicResource (queryEconomicResources (:toResourceInventoriedAs event))
        action (queryActions (:action event))
        economicEvent (createEconomicEvent event)
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
                                             (queryProcesses id)))
   :query/allProcesses (fn [_ _ _] (queryProcesses))
   :query/agent (fn [_ args _] (let [{:keys [id]} args]
                                           (queryAgents id)))
   :query/allAgents (fn [_ _ _] (queryAgents))
   :query/economicEvent (fn [_ args _] (let [{:keys [id]} args]
                                                   (queryEconomicEvents id)))
   :query/allEconomicEvents (fn [_ _ _] (queryEconomicEvents))
   :query/economicResource (fn [_ args _] (let [{:keys [id]} args]
                                                      (find-economicResource-by-id id)
                                                      ))
   :query/allEconomicResources (fn [_ _ _] (queryEconomicResources))
   :query/resourceSpecification (fn [_ args _] (let [{:keys [id]} args]
                                                           (queryResourceSpecifications id)
                                                           ))
   :query/allResourceSpecification (fn [_ _ _] (queryResourceSpecifications))
   :query/action (fn [_ args _] (let [{:keys [id]} args]
                                            (queryActions id)))
   :query/allActions (fn [_ _ _] (queryActions))
   :query/unit (fn [_ args _] (let [{:keys [id]} args]
                                          (queryUnits id)))
   :query/allUnits (fn [_ _ _] (queryUnits))
   :mutation/createEconomicEvent (fn [_ args _] (mutationNewEconomicEvent args))
   })

       (defn load-schema
       []
       (-> (io/resource "process-schema.edn")
           slurp
           edn/read-string
           (util/attach-resolvers (resolver-map))
           schema/compile))
