CLASSPATH := $(shell echo lib/*.jar | tr ' ' :)
MAIN_SRCS := $(shell find src/main/java -name '*.java')
TEST_SRCS := $(shell find src/test/java -name '*.java')
TEST_CLASSES := $(shell find src/test/java -name '*Test.java' \
                    | sed -e 's:src/test/java/::' -e 's/.java$$//' -e 's:/:.:g')

all: dist

dist: dist/ccs.jar dist/ccs-src.jar

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
	jar cf $@ -C classes/main .

dist/ccs-src.jar: dist/ccs.jar
	rm -f $@
	jar cf $@ -C src/main/java .

clean:
	rm -rf out
	rm -rf dist
	rm -rf classes

.PHONY: compile test dist clean
