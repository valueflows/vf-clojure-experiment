(ns vfprocess.db.queries
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :refer [insert!]]
            [honeysql.helpers :refer :all :as helpers]
            [honeysql.core :as sql]
            [next.jdbc.optional :as opt]))

(def ds
  {:dbtype   "sqlite"
   :host :none
   :dbname     "db/database.db"})

(def db (jdbc/get-datasource ds))


(defn query
  ([table id]
   (let [query (-> (select :*)
                   (merge-where [:= :id id])
                   (from table)
                   sql/format)]
     (jdbc/execute-one! db
                        query
                        {:builder-fn opt/as-unqualified-maps})))
  ([table]
   (let [query (-> (select :*)
                   (from table)
                   sql/format)]
     (jdbc/execute! db
                    query
                    {:builder-fn opt/as-unqualified-maps}))))


(defn query-economic-event
  [id]
   (let [event (jdbc/execute-one! db
                                  ["select * from EconomicEvent where id = ?" id]
                                  {:builder-fn opt/as-unqualified-maps})
         effortQuantityUnit (query :Unit (:resourceQuantityUnit event))
         action (query :Action (:action event))
         provider (query :Agent (:provider event))
         receiver (query :Agent (:receiver event))
         resourceInventoriedAs (query :EconomicResource (:resourceInventoriedAs event))
         resourceConformsTo  (query :ResourceSpecification (:resourceConformsTo event))
         inputOf (query :Process (:inputOf event))
         outputOf (query :Process (:outputOf event))]
     (-> event
         (merge {:inputOf inputOf})
         (merge {:outputOf outputOf})
         (merge {:effortQuantityUnit effortQuantityUnit})
         (merge {:action action})
         (merge {:provider provider})
         (merge {:receiver receiver})
         (merge {:resourceConformsTo resourceConformsTo})
         (merge {:resourceInventoriedAs resourceInventoriedAs}))))


(defn query-all-economic-event 
  []
  (let [ids (jdbc/execute! db
                           ["select id from EconomicEvent"]
                           {:builder-fn opt/as-unqualified-maps})]
    (map #(query-economic-event (:id %)) ids)))


(defn query-economic-resource
  [id]
  (let [resource (jdbc/execute-one! db
                                    ["select * from EconomicResource where id = ?" id]
                                    {:builder-fn opt/as-unqualified-maps})
        accountingQuantityUnit (query :Unit (:accountingQuantityUnit resource))
        unitOfEffort (query :Unit (:unitOfEffort resource))
        conformsTo (query :ResourceSpecification (:conformsTo resource))
        onHandQuantityUnit (query :Unit (:onHandQuantityUnit resource))
        owner (query :Agent (:owner resource))]
    (-> resource
        (merge {:accountingQuantityUnit accountingQuantityUnit})
        (merge {:unitOfEffort unitOfEffort})
        (merge {:conformsTo conformsTo})
        (merge {:onHandQuantityUnit onHandQuantityUnit})
        (merge {:owner owner}))))

(defn query-all-economic-resource 
  []
  (let [ids (jdbc/execute! db
                           ["select id from EconomicResource"]
                           {:builder-fn opt/as-unqualified-maps})]
    (map #(query-economic-resource (:id %)) ids)))


(defn get-process-inputs
  [id]
  (let [ids (jdbc/execute! db
                                     ["select id from EconomicEvent where inputOf = ?" id]
                                     {:builder-fn opt/as-unqualified-maps})]
    (map #(query-economic-event (:id %)) ids)))

(defn get-process-outputs
  [id]
  (let [ids (jdbc/execute! db
                                     ["select id from EconomicEvent where outputOf = ?" id]
                                     {:builder-fn opt/as-unqualified-maps})]
    (map #(query-economic-event (:id %)) ids)))


(defn get-agent-economic-event
  [id]
  (let [ids (jdbc/execute! db
                                     ["select id from EconomicEvent where provider = ?" id]
                                     {:builder-fn opt/as-unqualified-maps})]
    (map #(query-economic-event (:id %)) ids)))

(defn get-agent-inventoriedEconomicResource
  [id]
  (let [ids (jdbc/execute! db
                           ["select id from EconomicResource where owner = ?" id]
                           {:builder-fn opt/as-unqualified-maps})]
    (map #(query :EconomicResource (:id %)) ids)))


; ; MUTATIONS TODO
; (defn create-economic-event
;   [event]
;   (insert! db
;                 :EconomicEvent
;                 {:action (:action event)
;                  :resourceQuantityNumericValue (:resourceQuantityNumericValue event) 
;                  :resourceQuantityUnit (:resourceQuantityUnit event)
;                  :effortQuantityNumericValue (:effortQuantityNumericValue event)
;                  :effortQuantityUnit (:effortQuantityUnit event) 
;                  :hasPointInTime (:hasPointInTime event)
;                  :note (:note event)
;                  :provider (:provider event)
;                  :receiver (:receiver event)
;                  :resourceInventoriedAs (:resourceInventoriedAs event)
;                  :resourceConformsTo (:resourceConformsTo event)
;                  :inputOf (:inputOf event)
;                  :outputOf (:outputOf event)
;                  :toResourceInventoriedAs (:toResourceInventoriedAs event)}))
