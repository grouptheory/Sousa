select dropgeometrytable('raster');

create table raster
(
-- Note the use of the PostgreSQL extension, varchar without a length
-- specifier, indicating a string of any length.  PostgreSQL offers
-- another extension for strings of any length, text.  According to
-- the manual, several other SQL database management systems support
-- it.
	mapname varchar not null unique primary key,
-- western-most longitude
	west double precision,
-- northern-most latitude
	north double precision,
-- eastern-most longitude
	east double precision,
-- southern-most latitude
	south double precision
);

--select addgeometrycolumn('idol', 'raster', 'bounds', 4326, 'MULTIPOINT', 2);
--alter table raster alter column bounds set not null;

--create index raster_bounds_idx on rasterusing gist (bounds gist_geometry_ops);

alter table raster owner to idol;
