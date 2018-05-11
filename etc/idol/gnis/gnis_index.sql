-- Geometry checks

-- alter table geonames add constraint enforce_dims_lonlat
	-- check (ndims(lonlat) = 2);
-- alter table geonames add constraint enforce_srid_lonlat
	-- check (srid(lonlat) = 4326);
-- alter table geonames add constraint enforce_geotype_lonlat
	-- check ((geometrytype(lonlat) = 'POINT'::text) OR (lonlat is null));

-- Indices

-- create index geonames_lonlat_idx on geonames using gist (lonlat gist_geometry_ops);
create index geonames_lonlat_idx on geonames using gist (geomfromtext('POINT(' || lon || ' ' || lat || ')', 4326) gist_geometry_ops);

create index geonames_state on geonames (state);
create index geonames_feature_type on geonames (feature_type);
create index geonames_fips_state_fips_county on geonames (fips_state, fips_county);
create index geonames_feature_name on geonames (feature_name);

-- Foreign key refs.

-- alter table geonames add constraint fips_states_fk
	-- foreign key (fips_state) references fips_states (state_code)
	-- on delete set null on update cascade;
-- alter table geonames add constraint fips_counties_fk
	-- foreign key (fips_state, fips_county) references fips_counties (state_code, county_code)
	-- on delete set null on update cascade;

-- Views

create view geonames_lonlat as select 
        feature_id,
        state,
        feature_name,
        feature_type,
        county_name,
        fips_state,
        fips_county,
        lat_dms,
        lon_dms,
        lat,
        lon,
        src_lat_dms,
        src_lon_dms,
        src_lat,
        src_lon,
        elev,
        pop,
        fed_status,
        cell_name,
	geomfromtext('POINT(' || lon || ' ' || lat || ')', 4326) as lonlat from geonames;

-- Geometry stats

vacuum full analyze;
