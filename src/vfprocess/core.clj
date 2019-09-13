(ns vfprocess.core
  (:require [vfprocess.db.traversal :refer [incoming-vf-dfs]]
            [vfprocess.db.queries :refer [db 
                                          queryEconomicResources
                                          queryProcesses
                                          queryAgents
                                          queryEconomicEvents
                                          queryResourceSpecifications
                                          queryUnits
                                          queryActions]])
  (:gen-class))


   (defn find-economicEvent-by-id [id]
     (let [event (queryEconomicEvents id)

           effortQuantityUnit (queryUnits (:unit event))

           action (queryActions (:action event))

           provider (queryAgents (:provider event))

           receiver (queryAgents (:receiver event))

           resourceInventoriedAs (queryEconomicResources (:resourceInventoriedAs event))

           resourceConformsTo  (queryResourceSpecifications (:resourceConformsTo event))

           inputOf (queryProcesses (:inputOf event))

           outputOf (queryProcesses (:outputOf event))]
       (-> event
           (merge {:inputOf inputOf})
           (merge {:outputOf outputOf})
           (merge {:effortQuantityUnit effortQuantityUnit})
           (merge {:action action})
           (merge {:provider provider})
           (merge {:receiver receiver})
           (merge {:resourceConformsTo resourceConformsTo})
           (merge {:resourceInventoriedAs resourceInventoriedAs}))))


   (defn find-economicResource-by-id [id]
     (let [vf (incoming-vf-dfs {:type (str "economicResource_" id)})
           resource (queryEconomicResources id)]
     (merge resource
            {:valueflows vf}
            )))
