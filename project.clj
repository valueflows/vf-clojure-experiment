(defproject vfprocess "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.walmartlabs/lacinia-pedestal "0.5.0"]
                 [io.aviso/logging "0.2.0"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [com.stuartsierra/component "0.3.2"]
                 [camel-snake-kebab "0.4.0"]
                 [seancorfield/next.jdbc "1.0.7"]
                 [org.xerial/sqlite-jdbc "3.23.1"]
                 [com.walmartlabs/lacinia "0.34.0"]]
  :repl-options {;; If nREPL takes too long to load it may timeout,
                 ;; increase this to wait longer before timing out.
                 ;; Defaults to 30000 (30 seconds)
                 :timeout 120000})
