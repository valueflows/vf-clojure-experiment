(ns vfprocess.db.queries
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.optional :as opt]
            [next.jdbc.specs :as specs]))

(specs/instrument) 
(def ds
  {:dbtype   "sqlite"
   :host :none
   :dbname     "db/database.db"})

(def db (jdbc/get-datasource ds))


(defn query
  ([table id]
    (jdbc/execute-one! db
                       [(str "select * from " table " where id = ?" id)]
                       {:builder-fn opt/as-unqualified-maps}))
  ([table]
   (jdbc/execute! db
                  [(str "select * from " table)]
                  {:builder-fn opt/as-unqualified-maps})))

; Query Process by id or returns all the processes
(defn query-process
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

(defn query-economic-events
  ([id]
   (let [event (jdbc/execute-one! db
                                  ["select * from EconomicEvent where id = ?" id]
                                  {:builder-fn opt/as-unqualified-maps})
         effortQuantityUnit (query "Unit" (:unit event))
         action (query "Action" (:action event))
         provider (query "Agent" (:provider event))
         receiver (query "Agent" (:receiver event))
         resourceInventoriedAs (query "EconomicResource" (:resourceInventoriedAs event))
         resourceConformsTo  (query "ResourceSpecification" (:resourceConformsTo event))
         inputOf (query-process (:inputOf event))
         outputOf (query-process (:outputOf event))]
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


(defn create-economic-event
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
