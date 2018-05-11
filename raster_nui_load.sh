#!/bin/sh

#dropdb raster_nui
#createdb -E LATIN1 -O idol -T gistemplate raster_nui

#psql -f raster_nui_create.sql raster_nui

for x in $@ ; do
	echo ${x}
	psql -c "COPY raster_nui(mapname, west, north, east, south) \
	FROM '${x}' WITH CSV;" idol || echo -n "...FAILED!";
done;

psql -f raster_nui_index.sql idol
