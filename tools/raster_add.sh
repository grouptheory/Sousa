#!/bin/sh
# Usage: raster_add.sh DBHOST DBNAME FILES
# Copies the contents of all FILES named on the command line
# into the raster table of the DBNAME database on the host DBHOST.
#
# Assumes that each file is a comma separated value (CSV) file
# of five fields per line:
#
# image file name, top, right, bottom left
#
# where (top, right) are the longitude and latitude of the upper right corner
# of the image file's bounding box
#
# and
#
# (bottom, left) are the lower left corner of the image file's bounding box.
#
# All units are in decimal degrees expressed as double precision real numbers.
# West longitude and south latitude are negative numbers.
# East longitude and north latitude are positive numbers.
#
DBHOST=$1
shift
DBNAME=$1
shift
cat $@ | psql -U idol -h $DBHOST -c "COPY raster(mapname, west, north, east, south) \
	FROM STDIN WITH CSV;" $DBNAME || echo -n "...FAILED!";
