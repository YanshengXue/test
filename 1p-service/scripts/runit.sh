#!/bin/bash

VERSION=1.0

export CLASSPATH=$CLASSPATH:@PROJECT_NAME@-@VERSION@.jar:./lib/*

# set the enviroment
if [ -r "./setenv.sh" ]; then
  . "./setenv.sh"
fi

# ensure eiddo repo cloned (if does not exists) or up-to-date (if exists)
if [ ! -d ./conf/.git ]; then
  #clone from slave ELB if no git repo yet (optionally provide branch name parameter -b <name>)
  git clone git://internal-eiddo-slave-1852879765.us-west-2.elb.amazonaws.com/1p-service ./conf
fi


# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

		Please set the JAVA_HOME variable in your environment to match the
		location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

	Please set the JAVA_HOME variable in your environment to match the
	location of your Java installation."
fi

DEPL_ENV="-Darchaius.deployment.serverId=$(ec2metadata --instance-id) -Darchaius.deployment.region=$(ec2metadata --availability-zone)"

exec "$JAVACMD" $JAVA_OPTS $DEPL_ENV -Xmx1400m -XX:MaxPermSize=256m com.thomsonreuters.server.ServerRunner 2>&1 | tee log/output.log
