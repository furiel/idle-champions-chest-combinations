# -*- mode: Makefile -*-

jar:
	clojure -Srepro -Sdeps '{:aliases {:uberjar {:replace-paths [] :replace-deps {uberdeps/uberdeps {:mvn/version "1.0.4"}}}}}' -M:uberjar -m uberdeps.uberjar --target target/idle-champions.jar --aliases ""
