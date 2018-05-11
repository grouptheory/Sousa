#!/bin/sh

DATADIR=/other/dkleiner/data/gnis/z

dropdb gnis
createdb -D cxfs_space -E LATIN1 -O idol -T gistemplate gnis

psql -f gnis_create.sql gnis

for x in `dir ${DATADIR}/*.TXT`; do
	echo -n ${x}
	sed 's/\\/ /g' ${x} | psql -c "copy geonames( \
		feature_id, state, feature_name, feature_type, county_name, fips_state, \
		fips_county, lat_dms, lon_dms, lat, lon, \
		src_lat_dms, src_lon_dms, src_lat, src_lon, \
		elev, pop, fed_status, cell_name) from stdin with delimiter '|' null ''" gnis || echo -n "...FAILED!";
	echo
done;

psql -f gnis_index.sql gnis
