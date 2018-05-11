#!/bin/sh
# Usage: raster_initial_load.sh DBHOST DBNAME FILES
#
#dropdb raster
#createdb -E LATIN1 -O idol -T gistemplate raster


DBHOST=$1
shift
DBNAME=$1
shift

psql -U idol -h $DBHOST -f raster.sql $DBNAME

#
# What's the relationship between west, north, east, south and the 
# corners of the bounding box?
#
# Corners are (west, south) and (east, north)
#
cat $@ | psql -U idol -h $DBHOST -c "COPY raster(mapname, west, north, east, south) \
	FROM STDIN WITH CSV;" $DBNAME || echo -n "...FAILED!";

psql -U idol -h $DBHOST -f raster_index.sql $DBNAME
