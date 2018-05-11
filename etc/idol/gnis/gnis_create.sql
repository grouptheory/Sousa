-- GNIS

create table geonames(
	feature_id bigint,
	state char(2),
	feature_name varchar(100),
	feature_type varchar(20),
	county_name varchar(100),
	fips_state char(2),
	fips_county char(3),
	lat_dms char(7),
	lon_dms char(8),
	lat numeric(10, 5),
	lon numeric(10, 5),
	src_lat_dms char(8),
	src_lon_dms char(9),
	src_lat numeric(10, 5),
	src_lon numeric(10, 5),
	elev int,
	pop int,
	fed_status varchar(100),
	cell_name varchar(100));

-- select AddGeometryColumn('geonames', 'lonlat', 4326, 'POINT', 2);

-- alter table geonames drop constraint "enforce_geotype_lonlat";
-- alter table geonames drop constraint "enforce_srid_lonlat";
-- alter table geonames drop constraint "enforce_dims_lonlat";

-- create function update_geonames_lonlat() returns trigger as '
-- BEGIN
	-- NEW.lonlat := GeometryFromText(''POINT('' || NEW.lon::text || '' '' || NEW.lat::text || '')'', 4326);
	-- RETURN NEW;
-- END;
-- ' language 'plpgsql';

-- create trigger geonames_lonlat before insert or update on geonames for each row
	-- execute procedure update_geonames_lonlat();
