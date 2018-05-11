#!/bin/sh
env LD_LIBRARY_PATH=./ext/@sys java -Djava.library.path=ext/@sys -cp lib/Policy.jar:lib/SOUSA.jar:lib/SOUSA_Util.jar:ext/log4j-1.2.4.jar:ext/norm4j.jar:ext/skaringa.jar:ext/commons-logging.jar mil.navy.nrl.cmf.policy.TestSerialization $@
