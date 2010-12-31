#!/bin/bash

LOG=/home/ubuntu/mysql-install.log

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

export LOG


echo "********** Installing MySQL database `date` **********"

# Install MySQL
echo "" 
echo "********** Running apt-get update **********" 
apt-get update 

echo "" 
echo "********** Installing mysql-server deb **********" 
echo "mysql-server mysql-server/root_password select $DB_PASSWORD" | debconf-set-selections 
echo "mysql-server mysql-server/root_password_again select $DB_PASSWORD" | debconf-set-selections 
apt-get -y install mysql-server 
echo "" 

if [ "true" = $SEPARATE_EBS ]; then
  echo "" 
  echo "********** Installing xfsprogs deb **********" 
  apt-get -y install xfsprogs 


  ############################################################################
  # Move mysql setup, log, and data files to Amazon Elastic Block Store (EBS) 
  echo "" 
  echo "********** Setting up XFS volume on EBS **********" 
  echo "********** Stopping MySQL **********" 
  service mysql stop 
  # Make sure mysql has really stopped
  sleep 10 


  echo "" 
  echo "********** Creating and mounting XFS/EBS volume **********" 
  modprobe xfs
  mkfs.xfs /dev/sdh 
  echo "/dev/sdh /vol xfs noatime 0 0" | tee -a /etc/fstab
  mkdir -v -m 000 /vol 
  mount -v /vol 


  ##------------------------------------------------------
  ## Step One:  Move db files to the xfs volume
  ##------------------------------------------------------
  echo "********** Moving MySQL files to XFS/EBS volume **********" 
  mkdir -v /vol/etc /vol/lib /vol/log 
  mv -v /etc/mysql     /vol/etc/ 
  mv -v /var/lib/mysql /vol/lib/ 
  mv -v /var/log/mysql /vol/log/ 

  mkdir -v /etc/mysql
  mkdir -v /var/lib/mysql 
  mkdir -v /var/log/mysql 

  echo "/vol/etc/mysql /etc/mysql     none bind" | tee -a /etc/fstab
  mount -v /etc/mysql

  echo "/vol/lib/mysql /var/lib/mysql none bind" | tee -a /etc/fstab
  mount -v /var/lib/mysql

  echo "/vol/log/mysql /var/log/mysql none bind" | tee -a /etc/fstab
  mount -v /var/log/mysql

  rm -v /vol/lib/mysql/ib_logfile0 /vol/lib/mysql/ib_logfile1
  rm -v /vol/lib/mysql/ibdata1

  echo "********** Starting MySQL **********" 
  service mysql start 

  sleep 10 

  echo "********** Setting up and moving MySQL files to XFS/EBS volume complete **********" 
fi

# Create database and and user for access
echo "" 
echo "********** Create and intialize database **********" 
echo "********** Creating database **********" 
cat <<EOF >create_db.sql
CREATE DATABASE webapp;
GRANT ALL ON webapp.* TO 'web'@'%' IDENTIFIED BY '$DB_PASSWORD';
EOF
mysql -p --user=root --password=$DB_PASSWORD < create_db.sql
rm -v create_db.sql

echo "********** Initializing database **********"
#read install dir
mysql -p --user=root --password=$DB_PASSWORD -D webapp < /home/ubuntu/webapp.sql

echo "********** Configuring MySQL **********"
mv /etc/mysql/my.cnf /etc/mysql/my.cnf.original
sed -e 's/^bind-address/# bind-address/' < /etc/mysql/my.cnf.original > /etc/mysql/my.cnf

# Restart MySQL for changes to take affect
echo "" 
echo "********** Restarting MySQL **********" 
service mysql stop 
service mysql start 

echo ""
echo "********** Installation of MySQL database on `date` **********"
echo ""
echo "" 
