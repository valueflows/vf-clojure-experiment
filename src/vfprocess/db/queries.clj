(ns vfprocess.db.queries
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.optional :as opt]
            [next.jdbc.specs :as specs]
            [camel-snake-kebab.core :as csk]))

(specs/instrument) 
(def ds
  {:dbtype   "sqlite"
   :host :none
   :dbname     "db/database.db"})

(def db (jdbc/get-datasource ds))

; Query EconomicResource by id or returns all the resources
(defn queryEconomicResources
  ([id]
   (jdbc/execute-one!
    db
    ["select * from EconomicResource where id = ?" id]
    {:builder-fn opt/as-unqualified-maps})
   )
  ([]
   (jdbc/execute! db
                  ["select * from EconomicResource"]
                  {:builder-fn opt/as-unqualified-maps})))


; Query Process by id or returns all the processes
(defn queryProcesses
  ([id]
    (let [process (jdbc/execute-one! db
                                     ["select * from Process where id = ?" id]
                                     {:builder-fn opt/as-unqualified-maps})
          inputs (jdbc/execute! db
                                    ["select * from EconomicEvent where inputOf = ?" id]
                                    {:builder-fn opt/as-unqualified-maps})

          outputs (jdbc/execute! db
                                    ["select * from EconomicEvent where outputOf = ?" id]
                                    {:builder-fn opt/as-unqualified-maps})]
      (-> process
          (merge {:inputs inputs})
          (merge {:outputs outputs}))))
  ([]
   (jdbc/execute! db
                  ["select * from Process"]
                  {:builder-fn opt/as-unqualified-maps}
                  )))

; Query Agent by id or returns all the agents
(defn queryAgents
  ([id]
    (jdbc/execute-one! db
                       ["select * from Agent where id = ?" id]
                       {:builder-fn opt/as-unqualified-maps}
                       
                       ))
  ([]
   (jdbc/execute! db
                  ["select * from Agent"]
                  {:builder-fn opt/as-unqualified-maps})))



(defn queryResourceSpecifications
  ([id]
    (jdbc/execute-one! db
                       ["select * from ResourceSpecification where id = ?" id]
                       {:builder-fn opt/as-unqualified-maps}
                       ))
  ([]
   (jdbc/execute! db
                  ["select * from ResourceSpecification"]
                  {:builder-fn opt/as-unqualified-maps}
                  )))

(defn queryActions
  ([id]
    (jdbc/execute-one! db
                       ["select * from Action where id = ?" id]
                       {:builder-fn opt/as-unqualified-maps}
                       ))
  ([]
   (jdbc/execute! db
                  ["select * from Action"]
                  {:builder-fn opt/as-unqualified-maps}
                  )))

(defn queryUnits
  ([id]
    (jdbc/execute-one! db
                       ["select * from Unit where id = ?" id]
                       {:builder-fn opt/as-unqualified-maps}
                       ))
  ([]
   (jdbc/execute! db
                  ["select * from Unit"]
                  {:builder-fn opt/as-unqualified-maps}
                  )))



(defn queryEconomicEvents
  ([id]
   (let [event (jdbc/execute-one! db
                                  ["select * from EconomicEvent where id = ?" id]
                                  {:builder-fn opt/as-unqualified-maps})
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
  ([]
   (jdbc/execute! db
                  ["select * from EconomicEvent"]
                  {:builder-fn opt/as-unqualified-maps})))


(defn createEconomicEvent
  [event]
  (sql/insert! db
                :EconomicEvent
                {:action (:action event)
                 :resourceQuantityNumericValue (:resourceQuantityNumericValue event) 
                 :resourceQuantityUnit (:resourceQuantityUnit event)
                 :effortQuantityNumericValue (:effortQuantityNumericValue event)
                 :effortQuantityUnit (:effortQuantityUnit event) 
                 :hasPointInTime (:hasPointInTime event)
                 :note (:note event)
                 :provider (:provider event)
                 :receiver (:receiver event)
                 :resourceInventoriedAs (:resourceInventoriedAs event)
                 :resourceConformsTo (:resourceConformsTo event)
                 :inputOf (:inputOf event)
                 :outputOf (:outputOf event)
                 :toResourceInventoriedAs (:toResourceInventoriedAs event)}))