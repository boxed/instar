.PHONY: test

test:
	lein pdo cljx auto, cljsbuild auto test
