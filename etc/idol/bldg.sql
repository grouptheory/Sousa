select dropgeometrytable('bldg');

create table bldg
(
	ogc_fid serial not null,
	ss_id numeric(4,0),
	openflight character(30)
);

select addgeometrycolumn('idol', 'bldg', 'wkb_geometry', 26718, 'POINT', 2);
alter table bldg alter column wkb_geometry set not null;

create index bldg_wkb_geometry_idx on bldg using
	gist (wkb_geometry gist_geometry_ops);
create index bldg_transform_wkb_geometry_idx on bldg using
	gist (transform(wkb_geometry, 4326) gist_geometry_ops);

create view bldg_latlon as
	select ogc_fid, transform(wkb_geometry, 4326) as wkb_geometry, ss_id,
		"replace"(openflight, 'flt', 'pfb') as filename from bldg;

alter table bldg owner to idol;
