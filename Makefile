CLASSPATH := $(shell echo lib/*.jar | tr ' ' :)
MAIN_SRCS := $(shell find src/main/java -name '*.java')
TEST_SRCS := $(shell find src/test/java -name '*.java')
TEST_CLASSES := $(shell find src/test/java -name '*Test.java' \
                    | sed -e 's:src/test/java/::' -e 's/.java$$//' -e 's:/:.:g')

dist: dist/ccs.jar

compile:
	rm -rf classes/main
	mkdir -p classes/main
	javac -classpath $(CLASSPATH) -d classes/main $(MAIN_SRCS)

test: compile
	rm -rf classes/test
	cp -r src/test/resources classes/test
	javac -classpath $(CLASSPATH):classes/main -d classes/test $(TEST_SRCS)
	java -classpath $(CLASSPATH):classes/main:classes/test org.junit.runner.JUnitCore $(TEST_CLASSES)

dist/ccs.jar: compile test
	rm -f $@
	mkdir -p `dirname $@`
	rm -rf classes/tmp
	cp -r classes/main classes/tmp
	(cd classes/tmp && jar xf ../../lib/sac.jar)
	(cd classes/tmp && jar xf ../../lib/flute.jar)
	rm -rf classes/tmp/META-INF
	jar cf classes/tmp.jar -C classes/tmp .
	java -jar lib/jarjar-1.1.jar process jarjar.spec classes/tmp.jar $@
	rm classes/tmp.jar

clean:
	rm -rf out
	rm -rf dist
	rm -rf classes

.PHONY: compile test dist clean
