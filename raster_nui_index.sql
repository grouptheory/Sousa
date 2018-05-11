-- Geometry checks

-- alter table raster_nui add constraint enforce_dims_lonlat
	-- check (ndims(lonlat) = 2);
-- alter table raster_nui add constraint enforce_srid_lonlat
	-- check (srid(lonlat) = 4326);
-- alter table raster_nui add constraint enforce_geotype_lonlat
	-- check ((geometrytype(lonlat) = 'POINT'::text) OR (lonlat is null));

-- Indices

drop index raster_nui_bounds_idx;

-- create index raster_nui_lonlat_idx on raster_nui using gist (lonlat gist_geometry_ops);
create index raster_nui_bounds_idx on raster_nui using gist (
   	   geomfromtext('MULTIPOINT(' || west || ' ' || south || ', ' || east || ' ' || north || ')', 4326) 
	   gist_geometry_ops );

-- Foreign key refs.

-- alter table raster_nui add constraint fips_states_fk
	-- foreign key (fips_state) references fips_states (state_code)
	-- on delete set null on update cascade;
-- alter table raster_nui add constraint fips_counties_fk
	-- foreign key (fips_state, fips_county) references fips_counties (state_code, county_code)
	-- on delete set null on update cascade;

-- Views
drop view raster_nui_lonlat;

create view raster_nui_lonlat as select 
	   mapname,
   	   geomfromtext('MULTIPOINT(' || west || ' ' || south || ', ' || east || ' ' || north || ')', 4326) 
	   as bounds from raster_nui;

-- Geometry stats

vacuum full analyze;
