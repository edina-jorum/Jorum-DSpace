drop view stats.v_files_coll;
drop view stats.v_files_comm;
drop view stats.v_files;
drop view stats.v_download_author;
drop view stats.v_view_author;
drop view stats.v_ycct;

alter table stats.ip_spider add column country_code character varying(5);
update stats.ip_spider set country_code = stats.process_country(ip);
alter table stats.ip_spider alter column country_code set not null;
alter table stats.ip_spider 
	add constraint ip_spider_country_fk foreign key (country_code) 
		references stats.country(code);