.PHONY: test

cljs-test:
	lein pdo cljx auto, cljsbuild auto test
