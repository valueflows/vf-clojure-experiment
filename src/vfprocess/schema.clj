(ns vfprocess.schema
  "Contains custom resolver and a function to provide the full schema"
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [next.jdbc.sql :as sql]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.resolve :refer [resolve-as]]
            
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
            [vfprocess.core :refer [find-economicEvent-by-id]]
            [clojure.edn :as edn]))


    (defn resolve-process-by-id
      [args]
      (let [{:keys [id]} args]
        (queryProcesses id)))

    (defn resolve-processes
      []
      (queryProcesses))

    (defn resolve-agent-by-id
      [args]
      (let [{:keys [id]} args]
        (queryAgents id)))

    (defn resolve-agents
      []
      (queryAgents))

    (defn resolve-economicEvent-by-id
      [args]
      (let [{:keys [id]} args]
        (find-economicEvent-by-id id)))

    (defn resolve-economicEvents
      []
      (queryEconomicEvents))

    (defn resolve-economicResource-by-id
      [args]
      (let [{:keys [id]} args]
        (queryEconomicResources id)))

    (defn resolve-economicResources
      []
      (queryEconomicResources))

    (defn resolve-resourceSpecification-by-id
      [args]
      (let [{:keys [id]} args]
        (queryResourceSpecifications id)))

    (defn resolve-resourceSpecifications
      []
      (queryResourceSpecifications))


    (defn resolve-action-by-id
      [args]
      (let [{:keys [id]} args]
        (queryActions id)))

    (defn resolve-actions
      []
      (queryActions))

    (defn resolve-unit-by-id
      [args]
      (let [{:keys [id]} args]
        (queryUnits id)))

    (defn resolve-units
      []
      (queryUnits))

    (defn incoming-valueflows 
      [args]
      (let [{:keys [id]} args
            node (first-neighbors id)
            resource (queryEconomicResources id)
            first-node {:type (str "economicResource_" id)
                        :text (str (:accountingQuantityNumericValue resource) " " (:name resource))}]
        (incoming-vf-dfs first-node)))


(defn mutationNewEconomicEvent
  [args]
    (let [{:keys [event]} args
          economicResource (queryEconomicResources (:resourceInventoriedAs event))
          action (queryActions (:action event))
          ]
      (createEconomicEvent event)
      (println event)
      (if (some? (:resourceInventoriedAs event))
          (if (= (:resourceEffect action) "+")
            (do
              (println "ciao")
              (sql/update! db :EconomicResource
                           {:accountingQuantityNumericValue (+
                                                             (:accountingQuantityNumericValue economicResource)
                                                             (:resourceQuantityNumericValue event))}
                           {:onhandQuantitynumericValue (+
                                                         (:accountingQuantityNumericValue economicResource)
                                                         (:resourceQuantityNumericValue event))}))
            (:resourceQuantityNumericValue (:resourceInventoriedAs event))))))
  

    (defn resolver-map
      []
      {:query/process (fn [context args value] (resolve-process-by-id args))
       :query/allProcesses (fn [context args value] (resolve-processes))
       :query/agent (fn [context args value] (resolve-agent-by-id args))
       :query/allAgents (fn [context args value] (resolve-agents))
       :query/economicEvent (fn [context args value] (resolve-economicEvent-by-id args))
       :query/allEconomicEvents (fn [context args value] (resolve-economicEvents))
       :query/economicResource (fn [context args value] (resolve-economicResource-by-id args))
       :query/allEconomicResources (fn [context args value] (resolve-economicResources))
       :query/resourceSpecification (fn [context args value] (resolve-resourceSpecification-by-id args))
       :query/allResourceSpecification (fn [context args value] (resolve-resourceSpecifications))
       :query/action (fn [context args value] (resolve-action-by-id args))
       :query/allActions (fn [context args value] (resolve-actions))
       :query/unit (fn [context args value] (resolve-unit-by-id args))
       :query/allUnits (fn [context args value] (resolve-units))
       :query/incomingValueflows (fn [context args value] (incoming-valueflows args))
       :mutation/createEconomicEvent (fn [context args value] (mutationNewEconomicEvent args))
       })

       (defn load-schema
       []
       (-> (io/resource "process-schema.edn")
           slurp
           edn/read-string
           (util/attach-resolvers (resolver-map))
           schema/compile))
