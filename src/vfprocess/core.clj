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


