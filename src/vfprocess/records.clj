(ns vfprocess.records)

(defrecord ProcessRecord [id name note before finished])
(defrecord EconomicResourceRecord [id name track accountingQuantityNumericValue accountingQuantityUnit onhandQuantityUnit onhandQuantityNumericValue unitOfEffort note conformsTo])
(defrecord EconomicEventRecord [id
                                note
                                hasPointInTime
                                provider
                                receiver
                                action
                                inputOf
                                outputOf
                                resourceQuantityNumericValue
                                resourcequantityunit
                                effortQuantityNumericValue
                                effortQuantityUnit
                                resourceInventoriedAs
                                resourceConformsTo])
