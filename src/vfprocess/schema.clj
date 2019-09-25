(ns vfprocess.schema
  "Contains custom resolver and a function to provide the full schema"
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [next.jdbc.sql :as sql]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.resolve :refer [resolve-as]]
            [com.walmartlabs.lacinia.schema :refer [tag-with-type]]

            [com.stuartsierra.component :as component]
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
    (do
      (-> economicResource
          (merge {:track incoming-valueflows})))))

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
          (do (sql/insert! db :EconomicResource economicResource))
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


(defn resolve-trace
  [_ _ _]
  [(tag-with-type )]
  )

(defn resolver-map
  []
  {:query/process (fn [context args value] (let [{:keys [id]} args]
                                             (queryProcesses id)))
   :query/allProcesses (fn [context args value] (queryProcesses))
   :query/agent (fn [context args value] (let [{:keys [id]} args]
                                           (queryAgents id)))
   :query/allAgents (fn [context args value] (queryAgents))
   :query/economicEvent (fn [context args value] (let [{:keys [id]} args]
                                                   (queryEconomicEvents id)))
   :query/allEconomicEvents (fn [context args value] (queryEconomicEvents))
   :query/economicResource (fn [context args value] (let [{:keys [id]} args]
                                                      (find-economicResource-by-id id)
                                                      ))
   :query/allEconomicResources (fn [context args value] (queryEconomicResources))
   :query/resourceSpecification (fn [context args value] (let [{:keys [id]} args]
                                                           (queryResourceSpecifications id)
                                                           ))
   :query/allResourceSpecification (fn [context args value] (queryResourceSpecifications))
   :query/action (fn [context args value] (let [{:keys [id]} args]
                                            (queryActions id)))
   :query/allActions (fn [context args value] (queryActions))
   :query/unit (fn [context args value] (let [{:keys [id]} args]
                                          (queryUnits id)))
   :query/allUnits (fn [context args value] (queryUnits))
   :mutation/createEconomicEvent (fn [context args value] (mutationNewEconomicEvent args))
   })

       (defn load-schema
       []
       (-> (io/resource "process-schema.edn")
           slurp
           edn/read-string
           (util/attach-resolvers (resolver-map))
           schema/compile))
