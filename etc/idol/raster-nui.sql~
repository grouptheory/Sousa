select dropgeometrytable('raster');

create table raster
(
	mapname varchar(50) not null unique primary key,
	nrows integer not null,
	ncolumns integer not null,
	minelev double precision not null,
	maxelev double precision not null
);

select addgeometrycolumn('idol', 'raster', 'bounds', 4326, 'MULTIPOINT', 2);
alter table raster alter column bounds set not null;

create index raster_bounds_idx on raster using gist (bounds gist_geometry_ops);

create index raster_minelev_idx on raster (minelev);
create index raster_maxelev_idx on raster (maxelev);
create index raster_elev_idx on raster (minelev, maxelev);

alter table raster owner to idol;
