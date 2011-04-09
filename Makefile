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
	mkdir -p classes/test
	javac -classpath $(CLASSPATH) -d classes/test $(TEST_SRCS)
	java -classpath $(CLASSPATH):classes/main:classes/test org.junit.runner.JUnitCore $(TEST_CLASSES)

dist/ccs.jar: compile test
	rm -f $@
	mkdir -p `dirname $@`
	jar cvf $@ -C classes/main .

clean:
	rm -rf dist
	rm -rf classes

.PHONY: compile test dist clean
