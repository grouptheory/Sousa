#!/bin/sh
env LD_LIBRARY_PATH=./ext/@sys java -Djdbc.drivers=org.postgresql.Driver -Djava.library.path=./ext/@sys -cp lib/IDOL.jar:lib/IDOL_util.jar:lib/SOUSA.jar:lib/SOUSA_util.jar:lib/service/IDOL_service.jar:lib/user/IDOL_user.jar:ext/log4j-1.2.4.jar:ext/postgresql.jar:ext/stk.jar:ext/postgis.jar:ext/norm4j.jar mil.navy.nrl.cmf.sousa.Test c 4247 $1 $2
