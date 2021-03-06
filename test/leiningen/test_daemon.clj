(ns leiningen.test-daemon
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [bond.james :as bond]
            [leiningen.core.main :refer (abort)]
            [leiningen.daemon :as daemon]
            [leiningen.daemon-starter :as starter]
            [leiningen.daemon.common :as common]))

(defmacro throw-on-abort [& body]
  ;;redefine leiningen.core.main/abort to throw, rather than System/exit, so
  ;;our tests continue running!
  `(with-redefs [abort (fn [msg#]
                         (throw (Exception. msg#)))]
     ~@body))

(defmacro with-no-spawn [& body]
  `(bond/with-stub [common/sh! daemon/wait-for-running]
     ~@body))

(deftest daemon-args-are-passed-to-do-start
  (with-no-spawn
    (throw-on-abort
     (let [project {:daemon {"foo" {:pidfile "foo.pid"
                                    :args ["bar" "baz"]}}}]
       (daemon/daemon project "start" "foo")
       (let [bash-cmd (str/join " " (-> common/sh! bond/calls first :args))]
         (is (re-find #"lein daemon-starter foo bar baz" bash-cmd)))))))

(deftest cmd-line-args-are-passed-to-do-start
  (with-no-spawn
    (let [project {:daemon {"foo" {:pidfile "foo.pid"}}}]
      (daemon/daemon project "start" "foo" "bar")
      (let [bash-cmd (str/join " " (-> common/sh! bond/calls first :args))]
        (is (re-find #"lein daemon-starter foo bar" bash-cmd))))))

(deftest daemon-cmd-line-args-are-combined
  (with-no-spawn
    (let [project {:daemon {"foo" {:pidfile "foo.pid"
                                   :args ["bar"]}}}]
      (daemon/daemon project "start" "foo" "baz")
      (let [bash-cmd (str/join " " (-> common/sh! bond/calls first :args))]
        (is (re-find #"lein daemon-starter foo bar baz" bash-cmd))))))

(deftest passing-string-foo-on-cmd-line-finds-keyword-foo
  (with-no-spawn
    (throw-on-abort
     (let [project {:daemon {:foo {:ns "foo.bar"}}}]
       (daemon/daemon project "start" "foo")
       (let [bash-cmd (str/join " " (-> common/sh! bond/calls first :args))]
         (is (re-find #"lein daemon-starter foo" bash-cmd)))))))

(deftest passing-string-foo-on-cmd-line-finds-string-foo
  (with-no-spawn
    (throw-on-abort
     (let [project {:daemon {"foo" {:ns "foo.bar"}}}]
       (daemon/daemon project "start" "foo")
       (let [bash-cmd (str/join " " (-> common/sh! bond/calls first :args))]
         (is (re-find #"lein daemon-starter foo" bash-cmd)))))))

(deftest daemon-starter-finds-string-info
  (starter/daemon-starter {:dependencies '[[org.clojure/clojure "1.4.0"]]
                           :source-paths ["./test/leiningen/src"]
                           :daemon {"foo" {:ns "bogus.main"}}} "foo"))

(deftest daemon-starter-finds-keyword-daemon
  (starter/daemon-starter {:dependencies '[[org.clojure/clojure "1.4.0"]]
                           :source-paths ["./test/leiningen/src"]
                           :daemon {:foo {:ns "bogus.main"}}} "foo"))
