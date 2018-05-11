drop table 'annotations';

create table annotations
(
	text varchar(50) not null,
	lat double precision not null,
	lon double precision not null,
	elev double precision not null,
	mint double precision not null,
	maxt double precision not null,
	constraint pk primary key (lat, lon, elev, text)
);

create index position_idx on annotations (lat, lon, elev);
create index time_idx on annotations (mint, maxt);
