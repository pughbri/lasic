#!/bin/bash

LOG=/home/ubuntu/web-app-install.log

if [ "root" != `whoami` ]; then
  sudo -E $0 >> $LOG 2>&1
  echo "Ran the script as root and now I'm back to `whoami`" >> $LOG 2>&1
  echo "" >> $LOG 2>&1
  echo "" >> $LOG 2>&1
  echo "" >> $LOG 2>&1
  exit 0;
else
  echo "I am `whoami` so I can run the good stuff" >> $LOG 2>&1
fi

echo "********** Installing java web application: `date` **********"

echo "" 
echo "********** Running apt-get update **********" 
apt-get update 

echo "" 
echo "********** Installing Java JDK & JRE **********" 
#setup so that the jdk install will not prompt
echo "sun-java6-jdk shared/accepted-sun-dlj-v1-1 select true" | debconf-set-selections 
echo "sun-java6-jre shared/accepted-sun-dlj-v1-1 select true" | debconf-set-selections 
apt-get -y install sun-java6-jdk 


echo "********** Installing tomcat **********" 
apt-get -y install tomcat6 tomcat6-common

echo "" 
echo "********** Installing web-app **********" 
cp /home/ubuntu/java-webapp.war /var/lib/tomcat6/webapps

# Set address of servers that we need access to in /etc/hosts based on
# the variable passed into the script by LASIC
if ! ( grep -i mysqlDB$ /etc/hosts > /dev/null )
then
  echo "********** Adding mysqlDB entry to /etc/hosts *********" 
  echo "`dig $DB +short | head -1` mysqlDB" | tee -a /etc/hosts
fi


echo "********** restarting tomcat  **********" 
/etc/init.d/tomcat6 restart

echo "" 
echo "********** Completed Installation of java web application; `date` **********" 
echo "" 
echo "" 

