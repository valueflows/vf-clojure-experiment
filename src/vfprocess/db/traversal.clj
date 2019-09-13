(ns vfprocess.db.traversal
  (:require [next.jdbc :as jdbc]
            [camel-snake-kebab.core :as csk]
            [next.jdbc.optional :as opt]
            [vfprocess.db.queries :refer [db queryEconomicResources queryProcesses]])
  )

(defn visited?
  "Predicate which returns true if the node v has been visited already, false otherwise."
  [v coll]
  (some #(= % v) coll))


(defn first-neighbors
  [v]
  (let [node (queryEconomicResources v)]
    {:type (str "outputOf_" (:id node))}))

(defn find-id
  [type]
  (.substring type (+ (.indexOf type "_") 1)))

(defn find-neighbors
  "Returns the sequence of neighbors for the given node"
  [v]
  (let [id (find-id (:type v))]
    (println id)
    (cond
      (.contains (:type v) "process")
      (let [node (jdbc/execute-one! db
                                    ["select * from EconomicEvent where inputOf = ? " id]
                                    {:builder-fn opt/as-unqualified-maps})]
        (if (= nil node)
          nil
          {:type (str "inputOf_" (:id node))
           :text (:name node)}))
      (.contains (:type v) "inputOf")
      (let [node (queryEconomicResources id)]
        (if (= nil node)
          nil
          {:type (str "economicResource_" (:id node))
           :text (str (:accountingQuantityNumericValue node) " " (:name node))}))

      (.contains (:type v) "outputOf")
      (let [node (queryProcesses id)]
        (if (= nil node)
          nil
          {:type (str "process_" (:id node))
           :text (:name node)}))

      (.contains (:type v) "economicResource")
      (let [node(jdbc/execute-one! db
                                   ["select * from EconomicEvent where resourceInventoriedAs = ?" id]
                                   {:builder-fn opt/as-unqualified-maps})]
        (println node)
        (if (= nil node)
          nil
          {:type (str "outputOf_" (:id node))
           :text (str (:name (:action node)) " " (:resourceQuantityNumericValue node) " " (:name (:resourceQuantityUnit node)) " of " (:name (:resourceInventoriedAs node)))}))
      :else nil)))

(defn incoming-vf-dfs
  "Traverses a graph in Depth First Search (DFS)"
  [v]
  (println v)
  (loop [stack   (vector v) ;; Use a stack to store nodes we need to explore
         visited []]        ;; A vector to store the sequence of visited nodes
    (if (empty? stack)      ;; Base case - return visited nodes if the stack is empty
      visited
      (let [v           (peek stack)
            neighbors   (find-neighbors v)
            new-stack   (if (= nil neighbors) [] (vector neighbors))]
        (if (= nil neighbors)
          (recur new-stack (conj visited neighbors))
          (recur new-stack (conj visited neighbors)))))))