drop trigger browse_aft_d on stats.browse;
drop function stats.browse_aft_d();

drop trigger search_aft_d on stats.search;
drop function stats.search_aft_d();

drop trigger view_aft_d on stats.view;
drop function stats.view_aft_d();

drop trigger download_aft_d on stats.download;
drop function stats.download_aft_d();

drop trigger collection2item_aft_iud on collection2item;
drop function stats.collection_changes();

drop trigger communities2item_aft_iud on communities2item;
drop function stats.community_changes();

drop trigger metadatavalue_aft_iud on metadatavalue;
drop function stats.metadata_changes();

drop trigger log_trigger_bef_i on stats.log;
drop function stats.log_trigger_bef_i();

drop table stats.log;

drop function stats.aggregate_browse(character varying, date, character varying);
drop function stats.aggregate_download(character varying, date, double precision, integer, character varying);
drop function stats.aggregate_search(character varying, date);
drop function stats.aggregate_view(character varying, date, integer, character varying);
drop function stats.aggregate(date, date);
drop function stats.aggregate();
drop function stats.aggregate_browse(date, date);
drop function stats.aggregate_download(date, date);
drop function stats.aggregate_search(date, date);
drop function stats.aggregate_view(date, date);
drop function stats.aggregatetotal();
drop function stats.getyearmonth(date);

drop table stats.coll2item_changes;
drop table stats.comm2item_changes;
drop table stats.metadata2item_changes;

drop function stats.process_browse(text, text, text, text);

drop table stats.browse_type_month;
drop table stats.browse_month;
drop table stats.browse;

drop function stats.is_institution(character varying);
drop function stats.process_country(character varying);

drop sequence stats.browse_seq;
drop sequence stats.coll2item_changes_seq;
drop sequence stats.comm2item_changes_seq;
drop sequence stats.metadata2item_changes_seq;

truncate table stats.control;
alter table stats.control add column closed date not null;
insert into stats.control values (1,null,to_date('01011900','ddmmyyyy'));

alter table stats.view add column spider boolean DEFAULT false;  

CREATE INDEX view_spider_idx
  ON stats.view
  USING btree
  (spider);

alter table stats.download add column spider boolean DEFAULT false;  

CREATE INDEX download_spider_idx
  ON stats.download
  USING btree
  (spider);

alter table stats.search add column spider boolean DEFAULT false;  

CREATE INDEX search_spider_idx
  ON stats.search
  USING btree
  (spider);

drop function stats.is_spider(character varying);
drop function stats.process_download(text, text, text, text);
drop function stats.process_view(text, text, text, text);

CREATE OR REPLACE VIEW stats.v_view AS
	select 	v.view_id,
			v.date,
			v.time,
			v.item_id,
			h.handle,
			v.session_id,
			v.user_id,
			v.ip,
			v.country_code
	from 	stats.view v,
			handle h
	where 	v.item_id = h.resource_id
	and 	h.resource_type_id = 2
	and 	v.aggregated = true
	and 	v.spider = false;

DROP VIEW stats.v_view_comm;

CREATE OR REPLACE VIEW stats.v_view_comm AS 
 SELECT v.view_id, v.handle, v.item_id, v.session_id, v.user_id, v.date, v."time", v.ip, v.country_code, ci.community_id
   FROM stats.v_view v, communities2item ci
  WHERE v.item_id = ci.item_id;

DROP VIEW stats.v_view_coll;

CREATE OR REPLACE VIEW stats.v_view_coll AS 
 SELECT v.view_id, v.handle, v.item_id, v.session_id, v.user_id, v.date, v."time", v.ip, v.country_code, ci.collection_id
   FROM stats.v_view v, collection2item ci
  WHERE v.item_id = ci.item_id;
  
alter table stats.view drop column handle;

alter table stats.search drop column results;

alter table stats.login drop column login_type;
alter table stats.login drop column reverse_domain;
alter table stats.login alter column user_id type varchar(64);
update stats.login set user_id = email;
alter table stats.login drop column email;

drop function stats.process_login(text, text, text, text);

drop table stats.workflow_claim;
drop sequence stats.workflow_claim_seq;

drop VIEW stats.v_workflow_comm;
drop VIEW stats.v_workflow;

alter table stats.workflow alter column user_id type varchar(64);
update stats.workflow set user_id = email;
alter table stats.workflow drop column email;
alter table stats.workflow drop column new_state;

truncate table stats.workflow_steps;
alter table stats.workflow_steps add column taken boolean not null;

insert into stats.workflow_steps values (0, 'Submission', true);
insert into stats.workflow_steps values (1, 'Step 1 (Review)', false);
insert into stats.workflow_steps values (2, 'Step 1 (Review)', true);
insert into stats.workflow_steps values (3, 'Step 2 (Check)', false);
insert into stats.workflow_steps values (4, 'Step 2 (Check)', true);
insert into stats.workflow_steps values (5, 'Step 3 (Final Edit)', false);
insert into stats.workflow_steps values (6, 'Step 3 (Final Edit)', true);

CREATE OR REPLACE VIEW stats.v_workflow AS 
 SELECT 
	ws.name as state_desc,
        w.state, '' AS owner, w.collection_id, c.name AS collection_name, dc.text_value AS title, cc.community_id, co.short_description AS community_short, (co.short_description::text || ' - '::text) || co.name::text AS community_name
   FROM workflowitem w, collection c, community2collection cc, community co, dcvalue dc, stats.workflow_steps ws
  WHERE w.owner IS NULL AND w.collection_id = c.collection_id AND c.collection_id = cc.collection_id AND cc.community_id = co.community_id AND w.item_id = dc.item_id AND dc.dc_type_id = 64
  AND w.state = ws.code
UNION ALL 
 SELECT 
	ws.name as state_desc,
        w.state, (ep.firstname::text || ' '::text) || ep.lastname::text AS owner, w.collection_id, c.name AS collection_name, dc.text_value AS title, cc.community_id, co.short_description AS community_short, (co.short_description::text || ' - '::text) || co.name::text AS community_name
   FROM workflowitem w, eperson ep, collection c, community2collection cc, community co, dcvalue dc, stats.workflow_steps ws
  WHERE w.owner IS NOT NULL AND w.owner = ep.eperson_id AND w.collection_id = c.collection_id AND c.collection_id = cc.collection_id AND cc.community_id = co.community_id AND w.item_id = dc.item_id AND dc.dc_type_id = 64
  AND w.state = ws.code;
  
CREATE OR REPLACE VIEW stats.v_workflow_comm AS 
 SELECT w.workflow_id, w.workflow_item_id, w.item_id, w.collection_id, w.old_state, w.session_id, w.user_id, w.date, w."time", w.ip, cc.community_id
   FROM stats.workflow w, community2collection cc
  WHERE w.collection_id = cc.collection_id;
  
CREATE INDEX workflow_date_idx
  ON stats.workflow
  USING btree
  (date);

CREATE SEQUENCE stats.search_words_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

CREATE TABLE stats.search_words
(
  search_words_id integer NOT NULL,
  search_id integer NOT NULL,
  word character varying(64),
  CONSTRAINT search_words_pkey PRIMARY KEY (search_words_id)
) 
WITH OIDS;

drop table stats.words;

CREATE TABLE stats.search_words_month
(
  yearmonth integer NOT NULL,
  "year" integer NOT NULL,
  word character varying(64) NOT NULL,
  value integer,
  CONSTRAINT words_pkey PRIMARY KEY (yearmonth, word)
) 
WITH OIDS;

drop function stats.item_access(integer);
drop function stats.spider_add(character varying, character varying);

CREATE OR REPLACE VIEW stats.v_files AS
 SELECT b.bitstream_format_id, b.size_bytes AS size, bf.mimetype, bf.short_description, ib.item_id
   FROM bitstream b, bitstreamformatregistry bf, bundle2bitstream bb, bundle bu, item2bundle ib, item i
  WHERE b.deleted = false AND b.bitstream_format_id = bf.bitstream_format_id AND b.bitstream_id = bb.bitstream_id AND bb.bundle_id = bu.bundle_id AND bu.name::text = 'ORIGINAL'::text AND bu.bundle_id = ib.bundle_id AND ib.item_id = i.item_id AND i.in_archive = true AND i.withdrawn = false;

CREATE OR REPLACE VIEW stats.v_files_coll AS
 SELECT v.bitstream_format_id, v.size, v.mimetype, v.short_description, v.item_id, c.collection_id
   FROM stats.v_files v, collection2item c
  WHERE v.item_id = c.item_id;

CREATE OR REPLACE VIEW stats.v_files_comm AS
 SELECT v.bitstream_format_id, v.size, v.mimetype, v.short_description, v.item_id, c.community_id
   FROM stats.v_files v, community2item c
  WHERE v.item_id = c.item_id;

create or replace view stats.v_itemsbydate as
select i.item_id, m.text_value as date_issued
from stats.v_item i, metadatavalue m
where i.item_id = m.item_id
and m.metadata_field_id = 15;

CREATE OR REPLACE VIEW stats.v_ycct AS
 SELECT substr(p.date_issued, 1, 4) AS "year", cc.community_id, ci.collection_id, t."type", count(*) AS number
   FROM stats.v_itemsbydate p, collection2item ci, community2collection cc, stats.v_item_type t
  WHERE p.item_id = ci.item_id AND ci.collection_id = cc.collection_id AND p.item_id = t.item_id
  GROUP BY substr(p.date_issued, 1, 4), cc.community_id, ci.collection_id, t."type"
  ORDER BY substr(p.date_issued, 1, 4), cc.community_id, ci.collection_id, t."type";

create or replace view stats.v_itemsbyauthor as
select i.item_id, m.text_value as author
from stats.v_item i, metadatavalue m
where i.item_id = m.item_id
and m.metadata_field_id = 3;

CREATE OR REPLACE VIEW stats.v_download_author AS
 SELECT d.download_id, d.bitstream_id, d.item_id, d.session_id, d.user_id, d.date, d."time", d.ip, d.country_code, d.relative_value, ia.author
   FROM stats.download d, stats.v_itemsbyauthor ia
  WHERE d.item_id = ia.item_id;

CREATE OR REPLACE VIEW stats.v_view_author AS
 SELECT v.view_id, v.handle, v.item_id, v.session_id, v.user_id, v.date, v."time", v.ip, v.country_code, ia.author
   FROM stats.v_view v, stats.v_itemsbyauthor ia
  WHERE v.item_id = ia.item_id;

create or replace view stats.v_item2bitstream as
	select  i.item_id,
	        bi.bitstream_id
	from    item i,
	        item2bundle ib,
	        bundle bu,
	        bundle2bitstream bb,
	        bitstream bi
	where   i.item_id = ib.item_id
	and     ib.bundle_id = bu.bundle_id
	and     bu.name = 'ORIGINAL'
	and     bu.bundle_id = bb.bundle_id
	and     bb.bitstream_id = bi.bitstream_id;

create or replace view stats.z_today_date as
	select current_date as today_date;
	

create or replace view stats.z_view_unagg_month as
	select 	date_trunc('month',date) as month_trunc,
			count(*) as value
	from 	stats.view
	where 	aggregated=false and spider=false
	and 	date < current_date
	group by date_trunc('month',date);

create or replace view stats.z_view_unagg_country_month as
	select 	country_code, 
			date_trunc('month',date) as month_trunc, 
			count(*) as value 
	from 	stats.view
	where 	aggregated=false and spider=false
	and 	date < current_date
	group by country_code, date_trunc('month',date);

create or replace view stats.z_view_unagg_item_month as
	select 	item_id, 
			date_trunc('month',date) as month_trunc, 
			count(*) as value 
	from 	stats.view
	where 	aggregated=false and spider=false
	and 	date < current_date
	group by item_id, date_trunc('month',date);

create or replace view stats.z_view_unagg_comm_month as
	select 	ci.community_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value 
	from 	stats.view v, 
			communities2item ci
	where 	v.item_id = ci.item_id
	and 	aggregated=false
	and 	date < current_date
	group by ci.community_id, date_trunc('month',date);

create or replace view stats.z_view_unagg_coll_month as
	select 	ci.collection_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value 
	from 	stats.view v, 
			collection2item ci
	where 	v.item_id = ci.item_id
	and 	aggregated=false
	and 	date < current_date
	group by ci.collection_id, date_trunc('month',date);
	
create or replace view stats.z_view_unagg_country_comm_month as
	select 	ci.community_id,
			country_code,
			date_trunc('month',date) as month_trunc,
			count(*) as value 
	from 	stats.view v, 
			communities2item ci
	where 	v.item_id = ci.item_id
	and 	aggregated=false
	and 	date < current_date
	group by ci.community_id, country_code, date_trunc('month',date);

create or replace view stats.z_view_unagg_country_coll_month as
	select 	ci.collection_id,
			country_code,
			date_trunc('month',date) as month_trunc,
			count(*) as value 
	from 	stats.view v, 
			collection2item ci
	where 	v.item_id = ci.item_id
	and 	aggregated=false
	and 	date < current_date
	group by ci.collection_id, country_code, date_trunc('month',date);
	
create or replace view stats.z_view_unagg_item_comm_month as
	select 	ci.community_id,
			v.item_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value 
	from 	stats.view v, 
			communities2item ci
	where 	v.item_id = ci.item_id
	and 	aggregated=false
	and 	date < current_date
	group by ci.community_id, v.item_id, date_trunc('month',date);

create or replace view stats.z_view_unagg_item_coll_month as
	select 	ci.collection_id,
			v.item_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value 
	from 	stats.view v, 
			collection2item ci
	where 	v.item_id = ci.item_id
	and 	aggregated=false
	and 	date < current_date
	group by ci.collection_id, v.item_id, date_trunc('month',date);

create or replace view stats.z_view_unagg_metadata_month_1 as
	select 	m.metadata_field_id as field_id, 
			m.text_value as field_value, 
			date_trunc('month',date) as month_trunc,
			count(*) as value
	from 	stats.view v, 
			metadatavalue m, 
			stats.metadata_aggreg g
	where 	v.item_id = m.item_id
	and 	m.metadata_field_id = g.metadata_field_id
	and 	g.metadata_field_id != 15
	and 	aggregated=false
	and 	date < current_date
	group by m.metadata_field_id, m.text_value, date_trunc('month',date);

create or replace view stats.z_view_unagg_metadata_month_2 as
	select 	m.metadata_field_id as field_id, 
			substr(m.text_value,1,4) as field_value, 
			date_trunc('month',date) as month_trunc,
			count(*) as value
	from 	stats.view v, 
			metadatavalue m, 
			stats.metadata_aggreg g
	where 	v.item_id = m.item_id
	and 	m.metadata_field_id = g.metadata_field_id
	and 	g.metadata_field_id = 15
	and 	aggregated=false
	and 	date < current_date
	group by m.metadata_field_id, substr(m.text_value,1,4), date_trunc('month',date);

create or replace view stats.z_view_unagg_metadata_comm_month_1 as
	select 	m.metadata_field_id as field_id, 
			m.text_value as field_value,
			ci.community_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value
	from 	stats.view v, 
			metadatavalue m, 
			stats.metadata_aggreg g,
			communities2item ci
	where 	v.item_id = m.item_id
	and 	v.item_id = ci.item_id
	and 	m.metadata_field_id = g.metadata_field_id
	and 	g.metadata_field_id != 15
	and 	aggregated=false
	and 	date < current_date
	group by m.metadata_field_id, m.text_value, ci.community_id, date_trunc('month',date);
	
create or replace view stats.z_view_unagg_metadata_comm_month_2 as
	select 	m.metadata_field_id as field_id, 
			substr(m.text_value,1,4) as field_value,
			ci.community_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value
	from 	stats.view v, 
			metadatavalue m, 
			stats.metadata_aggreg g,
			communities2item ci
	where 	v.item_id = m.item_id
	and 	v.item_id = ci.item_id
	and 	m.metadata_field_id = g.metadata_field_id
	and 	g.metadata_field_id = 15
	and 	aggregated=false
	and 	date < current_date
	group by m.metadata_field_id, substr(m.text_value,1,4), ci.community_id, date_trunc('month',date);

create or replace view stats.z_view_unagg_metadata_coll_month_1 as
	select 	m.metadata_field_id as field_id, 
			m.text_value as field_value,
			ci.collection_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value
	from 	stats.view v, 
			metadatavalue m, 
			stats.metadata_aggreg g,
			collection2item ci
	where 	v.item_id = m.item_id
	and 	v.item_id = ci.item_id
	and 	m.metadata_field_id = g.metadata_field_id
	and 	g.metadata_field_id != 15
	and 	aggregated=false
	and 	date < current_date
	group by m.metadata_field_id, m.text_value, ci.collection_id, date_trunc('month',date);

create or replace view stats.z_view_unagg_metadata_coll_month_2 as
	select 	m.metadata_field_id as field_id, 
			substr(m.text_value,1,4) as field_value,
			ci.collection_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value
	from 	stats.view v, 
			metadatavalue m, 
			stats.metadata_aggreg g,
			collection2item ci
	where 	v.item_id = m.item_id
	and 	v.item_id = ci.item_id
	and 	m.metadata_field_id = g.metadata_field_id
	and 	g.metadata_field_id = 15
	and 	aggregated=false
	and 	date < current_date
	group by m.metadata_field_id, substr(m.text_value,1,4), ci.collection_id, date_trunc('month',date);

create or replace view stats.z_download_unagg_month as
	select 	date_trunc('month',date) as month_trunc,
			count(*) as value,
			sum(relative_value) as relative_value
	from 	stats.download
	where 	aggregated=false and spider=false
	and 	date < current_date
	group by date_trunc('month',date);

create or replace view stats.z_download_unagg_country_month as
	select 	country_code, 
			date_trunc('month',date) as month_trunc, 
			count(*) as value, sum(relative_value) as relative_value
	from 	stats.download
	where 	aggregated=false and spider=false
	and 	date < current_date
	group by country_code, date_trunc('month',date);

create or replace view stats.z_download_unagg_item_month as
	select 	item_id, 
			date_trunc('month',date) as month_trunc, 
			count(*) as value, sum(relative_value) as relative_value 
	from 	stats.download
	where 	aggregated=false and spider=false
	and 	date < current_date
	group by item_id, date_trunc('month',date);

create or replace view stats.z_download_unagg_comm_month as
	select 	ci.community_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value, sum(relative_value) as relative_value 
	from 	stats.download v, 
			communities2item ci
	where 	v.item_id = ci.item_id
	and 	aggregated=false
	and 	date < current_date
	group by ci.community_id, date_trunc('month',date);

create or replace view stats.z_download_unagg_coll_month as
	select 	ci.collection_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value, sum(relative_value) as relative_value 
	from 	stats.download v, 
			collection2item ci
	where 	v.item_id = ci.item_id
	and 	aggregated=false
	and 	date < current_date
	group by ci.collection_id, date_trunc('month',date);
	
create or replace view stats.z_download_unagg_country_comm_month as
	select 	ci.community_id,
			country_code,
			date_trunc('month',date) as month_trunc,
			count(*) as value, sum(relative_value) as relative_value 
	from 	stats.download v, 
			communities2item ci
	where 	v.item_id = ci.item_id
	and 	aggregated=false
	and 	date < current_date
	group by ci.community_id, country_code, date_trunc('month',date);

create or replace view stats.z_download_unagg_country_coll_month as
	select 	ci.collection_id,
			country_code,
			date_trunc('month',date) as month_trunc,
			count(*) as value, sum(relative_value) as relative_value 
	from 	stats.download v, 
			collection2item ci
	where 	v.item_id = ci.item_id
	and 	aggregated=false
	and 	date < current_date
	group by ci.collection_id, country_code, date_trunc('month',date);
	
create or replace view stats.z_download_unagg_item_comm_month as
	select 	ci.community_id,
			v.item_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value, sum(relative_value) as relative_value 
	from 	stats.download v, 
			communities2item ci
	where 	v.item_id = ci.item_id
	and 	aggregated=false
	and 	date < current_date
	group by ci.community_id, v.item_id, date_trunc('month',date);

create or replace view stats.z_download_unagg_item_coll_month as
	select 	ci.collection_id,
			v.item_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value, sum(relative_value) as relative_value 
	from 	stats.download v, 
			collection2item ci
	where 	v.item_id = ci.item_id
	and 	aggregated=false
	and 	date < current_date
	group by ci.collection_id, v.item_id, date_trunc('month',date);

create or replace view stats.z_download_unagg_metadata_month_1 as
	select 	m.metadata_field_id as field_id, 
			m.text_value as field_value, 
			date_trunc('month',date) as month_trunc,
			count(*) as value, sum(relative_value) as relative_value
	from 	stats.download v, 
			metadatavalue m, 
			stats.metadata_aggreg g
	where 	v.item_id = m.item_id
	and 	m.metadata_field_id = g.metadata_field_id
	and 	g.metadata_field_id != 15
	and 	aggregated=false
	and 	date < current_date
	group by m.metadata_field_id, m.text_value, date_trunc('month',date);

create or replace view stats.z_download_unagg_metadata_month_2 as
	select 	m.metadata_field_id as field_id, 
			substr(m.text_value,1,4) as field_value, 
			date_trunc('month',date) as month_trunc,
			count(*) as value, sum(relative_value) as relative_value
	from 	stats.download v, 
			metadatavalue m, 
			stats.metadata_aggreg g
	where 	v.item_id = m.item_id
	and 	m.metadata_field_id = g.metadata_field_id
	and 	g.metadata_field_id = 15
	and 	aggregated=false
	and 	date < current_date
	group by m.metadata_field_id, substr(m.text_value,1,4), date_trunc('month',date);

create or replace view stats.z_download_unagg_metadata_comm_month_1 as
	select 	m.metadata_field_id as field_id, 
			m.text_value as field_value,
			ci.community_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value, sum(relative_value) as relative_value
	from 	stats.download v, 
			metadatavalue m, 
			stats.metadata_aggreg g,
			communities2item ci
	where 	v.item_id = m.item_id
	and 	v.item_id = ci.item_id
	and 	m.metadata_field_id = g.metadata_field_id
	and 	g.metadata_field_id != 15
	and 	aggregated=false
	and 	date < current_date
	group by m.metadata_field_id, m.text_value, ci.community_id, date_trunc('month',date);
	
create or replace view stats.z_download_unagg_metadata_comm_month_2 as
	select 	m.metadata_field_id as field_id, 
			substr(m.text_value,1,4) as field_value,
			ci.community_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value, sum(relative_value) as relative_value
	from 	stats.download v, 
			metadatavalue m, 
			stats.metadata_aggreg g,
			communities2item ci
	where 	v.item_id = m.item_id
	and 	v.item_id = ci.item_id
	and 	m.metadata_field_id = g.metadata_field_id
	and 	g.metadata_field_id = 15
	and 	aggregated=false
	and 	date < current_date
	group by m.metadata_field_id, substr(m.text_value,1,4), ci.community_id, date_trunc('month',date);

create or replace view stats.z_download_unagg_metadata_coll_month_1 as
	select 	m.metadata_field_id as field_id, 
			m.text_value as field_value,
			ci.collection_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value, sum(relative_value) as relative_value
	from 	stats.download v, 
			metadatavalue m, 
			stats.metadata_aggreg g,
			collection2item ci
	where 	v.item_id = m.item_id
	and 	v.item_id = ci.item_id
	and 	m.metadata_field_id = g.metadata_field_id
	and 	g.metadata_field_id != 15
	and 	aggregated=false
	and 	date < current_date
	group by m.metadata_field_id, m.text_value, ci.collection_id, date_trunc('month',date);

create or replace view stats.z_download_unagg_metadata_coll_month_2 as
	select 	m.metadata_field_id as field_id, 
			substr(m.text_value,1,4) as field_value,
			ci.collection_id,
			date_trunc('month',date) as month_trunc,
			count(*) as value, sum(relative_value) as relative_value
	from 	stats.download v, 
			metadatavalue m, 
			stats.metadata_aggreg g,
			collection2item ci
	where 	v.item_id = m.item_id
	and 	v.item_id = ci.item_id
	and 	m.metadata_field_id = g.metadata_field_id
	and 	g.metadata_field_id = 15
	and 	aggregated=false
	and 	date < current_date
	group by m.metadata_field_id, substr(m.text_value,1,4), ci.collection_id, date_trunc('month',date);
	
create or replace view stats.z_search_unagg_month as
	select 	date_trunc('month',date) as month_trunc,
			count(*) as value
	from 	stats.search
	where 	aggregated=false and spider=false
	and 	date < current_date
	group by date_trunc('month',date);

create or replace view stats.z_search_unagg_words_month as
	select 	date_trunc('month',date) as month_trunc,
			w.word as word,
			count(*) as value
	from 	stats.search s, stats.search_words w
	where 	s.search_id = w.search_id
	and 	aggregated=false and spider=false
	and 	date < current_date
	group by date_trunc('month',date), word;

CREATE OR REPLACE VIEW stats.v_download AS
	select 	v.download_id,
			v.date,
			v.time,
			v.bitstream_id,
			v.item_id,
			h.handle,
			v.session_id,
			v.user_id,
			v.ip,
			v.country_code,
			v.relative_value
	from 	stats.download v,
			handle h
	where 	v.item_id = h.resource_id
	and 	h.resource_type_id = 2
	and 	v.aggregated = true
	and 	v.spider = false;

CREATE OR REPLACE VIEW stats.v_search AS
	select 	v.search_id,
			v.date,
			v.time,
			v.scope,
			v.scope_id,
			v.query,
			v.session_id,
			v.user_id,
			v.ip,
			v.country_code
	from 	stats.search v
	where 	v.aggregated = true
	and 	v.spider = false;

drop function stats.process_workflow(text, text, text, text);
drop function stats.process_search(text, text, text, text);
drop function stats.process_query(date, text);
drop function stats.process_log(text, text, text, text);

truncate table stats.agent_staging;
alter table stats.agent_staging add type integer not null;

CREATE TABLE stats.ip_keepwatched
(
  ip character varying(64) NOT NULL,
  robotstxt integer NOT NULL DEFAULT 0,
  robottraps integer NOT NULL DEFAULT 0,
  manuals integer NOT NULL DEFAULT 0,
  last_agent character varying(512),
  CONSTRAINT ip_keepwatched_pk PRIMARY KEY (ip)
);

CREATE OR REPLACE VIEW stats.v_session_analysis AS 
 SELECT c.ip, c.country_code, c.date, c.hour::time without time zone AS hour, c.first, c.last, (c.last - c.first)::time without time zone AS "time", c.hits, c.dhits, ((c.last - c.first) / c.hits::double precision)::time without time zone AS hitcadency, c.sessions
   FROM ( SELECT hits.date, date_trunc('hour'::text, hits."time"::interval) AS hour, hits.ip, hits.country_code, min(hits."time") AS first, max(hits."time") AS last, count(*) AS hits, count(DISTINCT hits.object_id) AS dhits, count(DISTINCT hits.session_id) AS sessions
           FROM ( SELECT view.date, view."time", view.ip, view.country_code, view.session_id, view.item_id AS object_id
                   FROM stats.view, stats.control
                  WHERE view.date > control.closed
        UNION ALL 
                 SELECT download.date, download."time", download.ip, download.country_code, download.session_id, download.bitstream_id AS object_id
                   FROM stats.download, stats.control
                  WHERE download.date > control.closed) hits
          GROUP BY hits.date, date_trunc('hour'::text, hits."time"::interval), hits.ip, hits.country_code
         HAVING count(*) > 1) c
  WHERE ((c.last - c.first) / c.hits::double precision) < '00:01:00'::time without time zone::interval AND (c.sessions::double precision / c.hits::real * 100::double precision) > 95::double precision AND c.hits > 10
  ORDER BY c.ip, c.date, c.hour::time without time zone;

CREATE OR REPLACE VIEW stats.v_session_keepwatched_analysis AS 
 SELECT c.ip, c.country_code, c.date, c.hour::time without time zone AS hour, c.first, c.last, (c.last - c.first)::time without time zone AS "time", c.hits, c.dhits, ((c.last - c.first) / c.hits::double precision)::time without time zone AS hitcadency, c.sessions, c.sessions::double precision / c.hits::real * 100::double precision AS sessionsinhits
   FROM ( SELECT hits.date, date_trunc('hour'::text, hits."time"::interval) AS hour, hits.ip, hits.country_code, min(hits."time") AS first, max(hits."time") AS last, count(*) AS hits, count(DISTINCT hits.object_id) AS dhits, count(DISTINCT hits.session_id) AS sessions
           FROM ( SELECT v.date, v."time", v.ip, v.country_code, v.session_id, v.item_id AS object_id
                   FROM stats.view v, stats.control c, stats.ip_keepwatched k
                  WHERE v.date > c.closed AND v.ip::text = k.ip::text
        UNION ALL 
                 SELECT d.date, d."time", d.ip, d.country_code, d.session_id, d.bitstream_id AS object_id
                   FROM stats.download d, stats.control c, stats.ip_keepwatched k
                  WHERE d.date > c.closed AND d.ip::text = k.ip::text) hits
          GROUP BY hits.date, date_trunc('hour'::text, hits."time"::interval), hits.ip, hits.country_code
         HAVING count(*) > 1) c
  ORDER BY c.ip, c.date, c.hour::time without time zone;

CREATE TABLE stats."temp"
(
  "name" character varying(512)
);

insert into stats.temp values ('0.1_hseo(at)cs.rutgers.edu');
insert into stats.temp values ('192.comAgent');
insert into stats.temp values ('4anything.com');
insert into stats.temp values ('AbachoBOT');
insert into stats.temp values ('ABCdatos');
insert into stats.temp values ('abcdatos_botlink');
insert into stats.temp values ('abot/0.1');
insert into stats.temp values ('About/0.1libwww-perl/5.47');
insert into stats.temp values ('accoona');
insert into stats.temp values ('Accoona');
insert into stats.temp values ('AcoiRobot');
insert into stats.temp values ('Acoon');
insert into stats.temp values ('Acorn');
insert into stats.temp values ('admin@crawler.de');
insert into stats.temp values ('AESOP');
insert into stats.temp values ('AESOP_com_SpiderMan');
insert into stats.temp values ('Agadine');
insert into stats.temp values ('Agent-admin/');
insert into stats.temp values ('ah-ha.com crawler');
insert into stats.temp values ('AIBOT');
insert into stats.temp values ('aipbot');
insert into stats.temp values ('Aipbot');
insert into stats.temp values ('Aladin');
insert into stats.temp values ('Aleksika');
insert into stats.temp values ('AlkalineBOT');
insert into stats.temp values ('Allesklar');
insert into stats.temp values ('AltaVista');
insert into stats.temp values ('Amfibi');
insert into stats.temp values ('AmfibiBOT');
insert into stats.temp values ('Amibot');
insert into stats.temp values ('Amiga-AWeb/3.4.167SE');
insert into stats.temp values ('amzn_assoc');
insert into stats.temp values ('AnnoMille');
insert into stats.temp values ('AnswerBus');
insert into stats.temp values ('AnswerChase');
insert into stats.temp values ('AnzwersCrawl');
insert into stats.temp values ('A-Online Search');
insert into stats.temp values ('Apexoo');
insert into stats.temp values ('Aport');
insert into stats.temp values ('appie');
insert into stats.temp values ('Appie');
insert into stats.temp values ('Arachnoidea');
insert into stats.temp values ('arachnoidea@euroseek.net');
insert into stats.temp values ('Aranha');
insert into stats.temp values ('ArchitectSpider');
insert into stats.temp values ('ArchitextSpider');
insert into stats.temp values ('archive_org');
insert into stats.temp values ('archive.org');
insert into stats.temp values ('archive.org_bot');
insert into stats.temp values ('Arikus_Spider');
insert into stats.temp values ('ASAHA');
insert into stats.temp values ('Asahina');
insert into stats.temp values ('AskAboutOil');
insert into stats.temp values ('Asked');
insert into stats.temp values ('ask jeeves');
insert into stats.temp values ('ASPseek');
insert into stats.temp values ('ASPSeek');
insert into stats.temp values ('Asterias');
insert into stats.temp values ('Atlocal');
insert into stats.temp values ('AtlocalBot');
insert into stats.temp values ('Atomz');
insert into stats.temp values ('autohttp');
insert into stats.temp values ('AV Fetch 1.0');
insert into stats.temp values ('AVSearch');
insert into stats.temp values ('Axadine');
insert into stats.temp values ('axadine/  (Axadine Crawler; http://www.axada.de/;  )');
insert into stats.temp values ('AxmoRobot');
insert into stats.temp values ('BaboomBot');
insert into stats.temp values ('Baiduspider');
insert into stats.temp values ('BaiDuSpider');
insert into stats.temp values ('Balihoo');
insert into stats.temp values ('BanBots/1.2');
insert into stats.temp values ('BarraHomeCrawler');
insert into stats.temp values ('Bdcindexer');
insert into stats.temp values ('bdcindexer_2.6.2');
insert into stats.temp values ('BDFetch');
insert into stats.temp values ('BDNcentral Crawler v2.3');
insert into stats.temp values ('Beautybot');
insert into stats.temp values ('beautybot/1.0');
insert into stats.temp values ('BebopBot');
insert into stats.temp values ('BecomeBot');
insert into stats.temp values ('BigCliqueBOT');
insert into stats.temp values ('BigCliqueBOT/1.03-dev');
insert into stats.temp values ('BIGLOTRON');
insert into stats.temp values ('Bigsearch.ca');
insert into stats.temp values ('Bilbo');
insert into stats.temp values ('Bilgi');
insert into stats.temp values ('BilgiBetaBot');
insert into stats.temp values ('Bitacle');
insert into stats.temp values ('Blaiz-Bee');
insert into stats.temp values ('BlitzBOT');
insert into stats.temp values ('BlogBot');
insert into stats.temp values ('Bloglines');
insert into stats.temp values ('Blogpulse');
insert into stats.temp values ('BlogSearch');
insert into stats.temp values ('BlogsNowBot');
insert into stats.temp values ('blogWatcher_Spider');
insert into stats.temp values ('BlogzIce');
insert into stats.temp values ('boitho.com');
insert into stats.temp values ('BotSeer');
insert into stats.temp values ('BravoBrian');
insert into stats.temp values ('BruinBot');
insert into stats.temp values ('BSDSeek/1.0');
insert into stats.temp values ('BTbot');
insert into stats.temp values ('BuildCMS');
insert into stats.temp values ('BullsEye');
insert into stats.temp values ('bumblebee@relevare.com');
insert into stats.temp values ('BurstFindCrawler');
insert into stats.temp values ('Buscaplus');
insert into stats.temp values ('Carleson');
insert into stats.temp values ('carleson/1.0');
insert into stats.temp values ('Carnegie_Mellon_University');
insert into stats.temp values ('Catall Spider');
insert into stats.temp values ('Ccubee');
insert into stats.temp values ('cfetch/1.0');
insert into stats.temp values ('CipinetBot');
insert into stats.temp values ('ClariaBot/1.0');
insert into stats.temp values ('Claymont.com');
insert into stats.temp values ('CloakDetect');
insert into stats.temp values ('Clushbot');
insert into stats.temp values ('ActiveWorlds/3.');
insert into stats.temp values ('collage.cgi');
insert into stats.temp values ('cometrics-bot');
insert into stats.temp values ('Computer_and_Automation_Research');
insert into stats.temp values ('Comrite');
insert into stats.temp values ('contact/jylee@kies.co.kr');
insert into stats.temp values ('Convera');
insert into stats.temp values ('CoolBot');
insert into stats.temp values ('Cosmos');
insert into stats.temp values ('CougarSearch');
insert into stats.temp values ('Cowbot');
insert into stats.temp values ('CrawlConvera');
insert into stats.temp values ('Crawler');
insert into stats.temp values ('CrawlerBoy');
insert into stats.temp values ('crawler@brainbot.com');
insert into stats.temp values ('Crawler (cometsearch@cometsystems.com)');
insert into stats.temp values ('crawler@fast.no');
insert into stats.temp values ('Crawllybot');
insert into stats.temp values ('CreativeCommons');
insert into stats.temp values ('CrocCrawler');
insert into stats.temp values ('Custom Spider www.bisnisseek.com');
insert into stats.temp values ('CydralSpider');
insert into stats.temp values ('DaAdLe.com ROBOT/');
insert into stats.temp values ('DataFountains');
insert into stats.temp values ('DataparkSearch');
insert into stats.temp values ('DataSpear');
insert into stats.temp values ('DaviesBot');
insert into stats.temp values ('DaviesBot/1.7');
insert into stats.temp values ('dbDig');
insert into stats.temp values ('dCSbot/1.1');
insert into stats.temp values ('DeepIndex');
insert into stats.temp values ('DeepIndexer.ca');
insert into stats.temp values ('deepweb');
insert into stats.temp values ('Demo Bot DOT 16b');
insert into stats.temp values ('de.searchengine.comBot');
insert into stats.temp values ('dev-spider2.searchpsider.com');
insert into stats.temp values ('Diamond');
insert into stats.temp values ('Digger');
insert into stats.temp values ('Digimarc WebReader');
insert into stats.temp values ('DigOut4U');
insert into stats.temp values ('DittoSpyder');
insert into stats.temp values ('dloader(NaverRobot)/');
insert into stats.temp values ('DoCoMo');
insert into stats.temp values ('DreamCatcher');
insert into stats.temp values ('Drecombot');
insert into stats.temp values ('dtSearchSpider');
insert into stats.temp values ('Dumbot');
insert into stats.temp values ('dumrobo(NaverRobot)/');
insert into stats.temp values ('EARTHCOM');
insert into stats.temp values ('EasyDL');
insert into stats.temp values ('eBot');
insert into stats.temp values ('EchO!');
insert into stats.temp values ('egothor/3.0a (+http://www.xdefine.org/robot.html)');
insert into stats.temp values ('ejupiter');
insert into stats.temp values ('elfbot');
insert into stats.temp values ('EMPAS');
insert into stats.temp values ('Enterprise_Search');
insert into stats.temp values ('Envolk');
insert into stats.temp values ('erik@malfunction.org');
insert into stats.temp values ('EroCrawler');
insert into stats.temp values ('eseek-larbin_2.6.2');
insert into stats.temp values ('ESISmartSpider');
insert into stats.temp values ('ES.NET_Crawler');
insert into stats.temp values ('e-SocietyRobot');
insert into stats.temp values ('eStyleSearch 4');
insert into stats.temp values ('EuripBot');
insert into stats.temp values ('EvaalSE');
insert into stats.temp values ('Eventax');
insert into stats.temp values ('Everest-Vulcan');
insert into stats.temp values ('Everest-Vulcan Inc./0.1');
insert into stats.temp values ('Exabot');
insert into stats.temp values ('ExactSeek');
insert into stats.temp values ('exactseek-crawler-2.63');
insert into stats.temp values ('Exalead NG/MimeLive Client');
insert into stats.temp values ('Excalibur Internet Spider V6.5.4');
insert into stats.temp values ('Execrawl');
insert into stats.temp values ('ExperimentalHenrytheMiragoRobot');
insert into stats.temp values ('EyeCatcher');
insert into stats.temp values ('EZResult');
insert into stats.temp values ('Factbot');
insert into stats.temp values ('Fastbot');
insert into stats.temp values ('FastCrawler');
insert into stats.temp values ('Fast Crawler Gold Edition');
insert into stats.temp values ('FAST Data Search Crawler');
insert into stats.temp values ('FAST Enterprise Crawler');
insert into stats.temp values ('FAST FirstPage retriever');
insert into stats.temp values ('FAST MetaWeb Crawler');
insert into stats.temp values ('Fast PartnerSite Crawler');
insert into stats.temp values ('FastSearch-AllTheWeb.com');
insert into stats.temp values ('FAST-WebCrawler');
insert into stats.temp values ('favo.eu');
insert into stats.temp values ('Faxobot');
insert into stats.temp values ('Feed24.com');
insert into stats.temp values ('Feedfetcher-Google');
insert into stats.temp values ('Feed Seeker Bot');
insert into stats.temp values ('Feedster Crawler');
insert into stats.temp values ('Felix');
insert into stats.temp values ('Filangy');
insert into stats.temp values ('FindAnISP.com');
insert into stats.temp values ('Findexa');
insert into stats.temp values ('FineBot');
insert into stats.temp values ('Firefly');
insert into stats.temp values ('FirstGov.gov');
insert into stats.temp values ('Firstsbot');
insert into stats.temp values ('Flapbot');
insert into stats.temp values ('Flatlandbot');
insert into stats.temp values ('Fluffy the spider');
insert into stats.temp values ('Flunky');
insert into stats.temp values ('FocusedSampler/1.0');
insert into stats.temp values ('Fooky.com');
insert into stats.temp values ('Francis');
insert into stats.temp values ('Francis/1.0');
insert into stats.temp values ('FreeFind');
insert into stats.temp values ('FreshNotes');
insert into stats.temp values ('FuseBulb');
insert into stats.temp values ('FyberSearch');
insert into stats.temp values ('FyberSpider');
insert into stats.temp values ('Gaisbot');
insert into stats.temp values ('GAIS Robot');
insert into stats.temp values ('GalaxyBot');
insert into stats.temp values ('Gallent Search Spider');
insert into stats.temp values ('Gamekitbot');
insert into stats.temp values ('GammaSpider');
insert into stats.temp values ('gazz/1.0');
insert into stats.temp values ('gazz@nttrd.com');
insert into stats.temp values ('GenCrawler');
insert into stats.temp values ('generic_crawler/01.0217/');
insert into stats.temp values ('genieBot');
insert into stats.temp values ('GeonaBot');
insert into stats.temp values ('GigaBaz');
insert into stats.temp values ('GigaBlast');
insert into stats.temp values ('Gigabot');
insert into stats.temp values ('GigabotSiteSearch/2.0');
insert into stats.temp values ('Giskard');
insert into stats.temp values ('GNODSPIDER');
insert into stats.temp values ('Goblin');
insert into stats.temp values ('GoForIt');
insert into stats.temp values ('Gonzo');
insert into stats.temp values ('gonzo1');
insert into stats.temp values ('Goofer/0.2');
insert into stats.temp values ('Googlebot');
insert into stats.temp values ('googlebot@googlebot.com');
insert into stats.temp values ('Googlebot-Image/1.0');
insert into stats.temp values ('Googlebot/Test');
insert into stats.temp values ('GrigorBot');
insert into stats.temp values ('Gromit');
insert into stats.temp values ('grub-client');
insert into stats.temp values ('grub crawler(http://www.grub.org)');
insert into stats.temp values ('gsa-crawler');
insert into stats.temp values ('Gulliver');
insert into stats.temp values ('GurujiBot');
insert into stats.temp values ('HappyFunBot');
insert into stats.temp values ('Harvest-NG');
insert into stats.temp values ('Hatena');
insert into stats.temp values ('hbtronix.spider');
insert into stats.temp values ('HeinrichderMirago');
insert into stats.temp values ('Helix');
insert into stats.temp values ('HenriLeRobotMirago');
insert into stats.temp values ('HenrytheMirago');
insert into stats.temp values ('HenryTheMiragoRobot');
insert into stats.temp values ('Hippias');
insert into stats.temp values ('Hitwise Spider');
insert into stats.temp values ('holmes/');
insert into stats.temp values ('HomePageSearch(hpsearch.uni-trier.de)');
insert into stats.temp values ('Homerbot');
insert into stats.temp values ('Honda-Search');
insert into stats.temp values ('http://Ask.24x.Info/ (http://narres.it/)');
insert into stats.temp values ('http://www.abcdatos.com/botlink/');
insert into stats.temp values ('http://www.almaden.ibm.com');
insert into stats.temp values ('http://www.istarthere.com');
insert into stats.temp values ('http://www.monogol.de');
insert into stats.temp values ('http://www.trendtech.dk/spider.asp');
insert into stats.temp values ('Hubater');
insert into stats.temp values ('i1searchbot');
insert into stats.temp values ('ia_archiver');
insert into stats.temp values ('IAArchiver-1.0');
insert into stats.temp values ('iCCrawler');
insert into stats.temp values ('ichiro');
insert into stats.temp values ('IconSurf');
insert into stats.temp values ('ICRA_label_spider');
insert into stats.temp values ('icsbot-0.1');
insert into stats.temp values ('Ideare - SignSite');
insert into stats.temp values ('ideare - SignSite/1.x');
insert into stats.temp values ('IIITBOT');
insert into stats.temp values ('Ilial');
insert into stats.temp values ('IlseBot');
insert into stats.temp values ('IlTrovatore');
insert into stats.temp values ('ImageWalker');
insert into stats.temp values ('IncyWincy');
insert into stats.temp values ('IndexTheWeb.com');
insert into stats.temp values ('Inet library');
insert into stats.temp values ('InfoFly/1.0');
insert into stats.temp values ('INFOMINE');
insert into stats.temp values ('info@searchhippo.com');
insert into stats.temp values ('InfoSeek');
insert into stats.temp values ('htdig/3.1.');
insert into stats.temp values ('INGRID/3.0 MT');
insert into stats.temp values ('Inktomi');
insert into stats.temp values ('InnerpriseBot');
insert into stats.temp values ('Insitor');
insert into stats.temp values ('Insitornaut');
insert into stats.temp values ('Intelix');
insert into stats.temp values ('InternetArchive');
insert into stats.temp values ('Internet Ninja x.0');
insert into stats.temp values ('InternetSeer');
insert into stats.temp values ('ipiumBot');
insert into stats.temp values ('IPiumBot laurion(dot)com');
insert into stats.temp values ('IpselonBot');
insert into stats.temp values ('IRLbot');
insert into stats.temp values ('Iron33');
insert into stats.temp values ('Jabot');
insert into stats.temp values ('Jack');
insert into stats.temp values ('Jambot');
insert into stats.temp values ('jan.gelin@av.com');
insert into stats.temp values ('Jayde Crawler');
insert into stats.temp values ('jeeves');
insert into stats.temp values ('Jetbot');
insert into stats.temp values ('jyxobot');
insert into stats.temp values ('KE_1.0/2.0');
insert into stats.temp values ('Kenjin Spider');
insert into stats.temp values ('KFSW-Bot');
insert into stats.temp values ('Kinja');
insert into stats.temp values ('KIT-Fireball');
insert into stats.temp values ('KIT-Fireball/2.0');
insert into stats.temp values ('Knowledge.com');
insert into stats.temp values ('Kuloko');
insert into stats.temp values ('kulturarw3');
insert into stats.temp values ('LapozzBot');
insert into stats.temp values ('LEIA/');
insert into stats.temp values ('Libby_1.1/libwww-perl/5.47');
insert into stats.temp values ('libWeb/clsHTTP -- hiongun@kt.co.kr');
insert into stats.temp values ('linknzbot');
insert into stats.temp values ('LiveTrans');
insert into stats.temp values ('Llaut');
insert into stats.temp values ('LNSpiderguy');
insert into stats.temp values ('LocalBot/1.0 ( http://www.localbot.co.uk/)');
insert into stats.temp values ('LocalcomBot');
insert into stats.temp values ('Lockstep Spider');
insert into stats.temp values ('Look.com');
insert into stats.temp values ('lycos');
insert into stats.temp values ('Lycos_Spider');
insert into stats.temp values ('Mackster');
insert into stats.temp values ('Mag-Net');
insert into stats.temp values ('mailto:webcraft@bea.com');
insert into stats.temp values ('mammoth/1.0');
insert into stats.temp values ('MantraAgent');
insert into stats.temp values ('MapoftheInternet.com');
insert into stats.temp values ('mapper@teradex.com');
insert into stats.temp values ('Mariner/5.1b');
insert into stats.temp values ('Martini');
insert into stats.temp values ('MaSagool');
insert into stats.temp values ('MasterSeek');
insert into stats.temp values ('Mata Hari/2.00');
insert into stats.temp values ('Matrix S.p.A. - FAST Enterprise Crawler 6');
insert into stats.temp values ('Maxomobot');
insert into stats.temp values ('MediaCrawler');
insert into stats.temp values ('Mediapartners');
insert into stats.temp values ('Mediapartners-Google/2.1');
insert into stats.temp values ('MediaSearch/0.1');
insert into stats.temp values ('MegaSheep v1.0');
insert into stats.temp values ('Megite');
insert into stats.temp values ('Mercator');
insert into stats.temp values ('Metaeuro');
insert into stats.temp values ('MetagerBot');
insert into stats.temp values ('Metaspinner');
insert into stats.temp values ('Metaspinner/0.01');
insert into stats.temp values ('Metatagsdir');
insert into stats.temp values ('MFC_Tear_Sample');
insert into stats.temp values ('MicroBaz');
insert into stats.temp values ('MicrosoftPrototypeCrawler');
insert into stats.temp values ('Misterbot');
insert into stats.temp values ('Miva');
insert into stats.temp values ('Miva (AlgoFeedback@miva.com)');
insert into stats.temp values ('MJ12bot');
insert into stats.temp values ('MnogoSearch');
insert into stats.temp values ('moget@goo.ne.jp');
insert into stats.temp values ('mogimogi');
insert into stats.temp values ('MojeekBot');
insert into stats.temp values ('Mole2Morris - Mixcat Crawler (+http://mixcat.com)');
insert into stats.temp values ('Morris');
insert into stats.temp values ('mozDex');
insert into stats.temp values ('MP3Bot');
insert into stats.temp values ('MQbot');
insert into stats.temp values ('msnbot');
insert into stats.temp values ('Msnbot');
insert into stats.temp values ('MSNBOT/0.1');
insert into stats.temp values ('MSNPTC');
insert into stats.temp values ('MultiText');
insert into stats.temp values ('MusicWalker');
insert into stats.temp values ('Mylinea.com');
insert into stats.temp values ('Naamah');
insert into stats.temp values ('NABOT');
insert into stats.temp values ('NationalDirectory');
insert into stats.temp values ('NaverBot');
insert into stats.temp values ('NavissoBot');
insert into stats.temp values ('Nazilla');
insert into stats.temp values ('NCSA');
insert into stats.temp values ('Nebullabot');
insert into stats.temp values ('Netluchs');
insert into stats.temp values ('NetResearchServer');
insert into stats.temp values ('NetWhatCrawler');
insert into stats.temp values ('NextopiaBOT');
insert into stats.temp values ('NG/1.0');
insert into stats.temp values ('NG/4.0.1229');
insert into stats.temp values ('NG-Search');
insert into stats.temp values ('Noago');
insert into stats.temp values ('NokodoBot');
insert into stats.temp values ('Norbert the Spider(Burf.com)');
insert into stats.temp values ('Noxtrumbot');
insert into stats.temp values ('NP/0.1');
insert into stats.temp values ('NPBot');
insert into stats.temp values ('nuSearch');
insert into stats.temp values ('Nutch');
insert into stats.temp values ('NutchCVS/0.0x-dev');
insert into stats.temp values ('NutchOrg');
insert into stats.temp values ('NZBot');
insert into stats.temp values ('obidos-bot');
insert into stats.temp values ('ObjectsSearch');
insert into stats.temp values ('oBot');
insert into stats.temp values ('Ocelli');
insert into stats.temp values ('Octora');
insert into stats.temp values ('OliverPerry');
insert into stats.temp values ('omgilibot');
insert into stats.temp values ('OmniExplorer_Bot');
insert into stats.temp values ('Onet.pl');
insert into stats.temp values ('OntoSpider');
insert into stats.temp values ('Openbot');
insert into stats.temp values ('Opencola');
insert into stats.temp values ('Openfind');
insert into stats.temp values ('OpenISearch');
insert into stats.temp values ('OpenTaggerBot');
insert into stats.temp values ('OpenTextSiteCrawler');
insert into stats.temp values ('OpenWebSpider');
insert into stats.temp values ('Oracle Ultra Search');
insert into stats.temp values ('Overture-WebCrawler/');
insert into stats.temp values ('PageBites');
insert into stats.temp values ('Pagebull');
insert into stats.temp values ('parallelContextFocusCrawler');
insert into stats.temp values ('ParaSite');
insert into stats.temp values ('Patwebbot');
insert into stats.temp values ('pd02_1.0.0 pd02_1.0.0@dzimi@post.sk');
insert into stats.temp values ('peerbot');
insert into stats.temp values ('PEERbot');
insert into stats.temp values ('phortse@hanmail.net');
insert into stats.temp values ('PicoSearch');
insert into stats.temp values ('Piffany');
insert into stats.temp values ('pipeLiner');
insert into stats.temp values ('Pizilla++ ver 2.45');
insert into stats.temp values ('PJspider');
insert into stats.temp values ('PluckFeedCrawler');
insert into stats.temp values ('Pompos');
insert into stats.temp values ('Popdex');
insert into stats.temp values ('PortalBSpider/2.0');
insert into stats.temp values ('PROve AnswerBot');
insert into stats.temp values ('psbot');
insert into stats.temp values ('Psbot');
insert into stats.temp values ('psbot/0.1');
insert into stats.temp values ('Qango.com');
insert into stats.temp values ('QPCreep Test Rig');
insert into stats.temp values ('Quepasa');
insert into stats.temp values ('QuepasaCreep');
insert into stats.temp values ('Rabaz');
insert into stats.temp values ('rabaz (rabaz at gigabaz dot com)');
insert into stats.temp values ('RaBot');
insert into stats.temp values ('RAMPyBot');
insert into stats.temp values ('ReadABlog');
insert into stats.temp values ('Reaper');
insert into stats.temp values ('RixBot');
insert into stats.temp values ('roach.smo.av.com-1.0');
insert into stats.temp values ('RoboCrawl');
insert into stats.temp values ('RoboPal');
insert into stats.temp values (':robot/1.0');
insert into stats.temp values ('Robot@SuperSnooper.Com');
insert into stats.temp values ('Robot/www.pj-search.com');
insert into stats.temp values ('robot@xyleme.com');
insert into stats.temp values ('Robozilla/1.0');
insert into stats.temp values ('Rotondo/3.1 libwww/5.3.1');
insert into stats.temp values ('RRC (crawler_admin@bigfoot.com)');
insert into stats.temp values ('RSSMicro');
insert into stats.temp values ('RufusBot');
insert into stats.temp values ('ru-robot');
insert into stats.temp values ('SandCrawler');
insert into stats.temp values ('Savvybot');
insert into stats.temp values ('SBIder');
insert into stats.temp values ('ScanWeb');
insert into stats.temp values ('ScholarUniverse');
insert into stats.temp values ('schwarzmann.biz');
insert into stats.temp values ('ScollSpider');
insert into stats.temp values ('Scooter');
insert into stats.temp values ('scooter-venus-3.0.vns');
insert into stats.temp values ('ScoutAbout');
insert into stats.temp values ('Scoutmaster');
insert into stats.temp values ('Scrubby');
insert into stats.temp values ('search.at V1.2');
insert into stats.temp values ('search.ch');
insert into stats.temp values ('SearchdayBot');
insert into stats.temp values ('SearchExpress');
insert into stats.temp values ('SearchGuild');
insert into stats.temp values ('SearchSight');
insert into stats.temp values ('SearchSpider');
insert into stats.temp values ('SearchTone');
insert into stats.temp values ('sebastien.ailleret@inria.fr');
insert into stats.temp values ('Seekbot');
insert into stats.temp values ('Seeker.lookseek.com');
insert into stats.temp values ('Sensis');
insert into stats.temp values ('Seznam');
insert into stats.temp values ('SeznamBot');
insert into stats.temp values ('ShopWiki');
insert into stats.temp values ('ShopWiki/1.0');
insert into stats.temp values ('Shoula.com');
insert into stats.temp values ('SightQuestBot/');
insert into stats.temp values ('silk/1.0');
insert into stats.temp values ('SiteSpider');
insert into stats.temp values ('SiteTruth.com');
insert into stats.temp values ('SiteXpert');
insert into stats.temp values ('Skampy');
insert into stats.temp values ('Skimpy');
insert into stats.temp values ('Slarp/0.1');
insert into stats.temp values ('Slider_Search_v1-de');
insert into stats.temp values ('Slurp');
insert into stats.temp values ('slurp@inktomi');
insert into stats.temp values ('SlySearch');
insert into stats.temp values ('SnykeBot');
insert into stats.temp values ('Speedfind');
insert into stats.temp values ('Speedy Spider');
insert into stats.temp values ('Speedy_Spider');
insert into stats.temp values ('Spida/0.1');
insert into stats.temp values ('spider@aeneid.com');
insert into stats.temp values ('SpiderMan');
insert into stats.temp values ('SpiderMonkey');
insert into stats.temp values ('Spider-Sleek');
insert into stats.temp values ('Spider TraficDublu');
insert into stats.temp values ('spider.yellopet.com');
insert into stats.temp values ('sportsuchmaschine.de');
insert into stats.temp values ('sproose');
insert into stats.temp values ('Sqworm');
insert into stats.temp values ('StackRambler');
insert into stats.temp values ('Steeler');
insert into stats.temp values ('Strategic Board Bot');
insert into stats.temp values ('suchbaer.de');
insert into stats.temp values ('suchbot');
insert into stats.temp values ('Suchbot');
insert into stats.temp values ('Suchknecht.at');
insert into stats.temp values ('suchpad/1.0');
insert into stats.temp values ('Suchpadbot');
insert into stats.temp values ('support@canseek.ca');
insert into stats.temp values ('Swooglebot');
insert into stats.temp values ('SygolBot');
insert into stats.temp values ('SynoBot');
insert into stats.temp values ('Synoo');
insert into stats.temp values ('Syntryx');
insert into stats.temp values ('Szukacz');
insert into stats.temp values ('tags2dir');
insert into stats.temp values ('Talkro Web-Shot');
insert into stats.temp values ('TCDBOT');
insert into stats.temp values ('TECOMAC-Crawler');
insert into stats.temp values ('Tecomi Bot');
insert into stats.temp values ('Teoma');
insert into stats.temp values ('teoma_admin@hawkholdings.com');
insert into stats.temp values ('teoma_agent1');
insert into stats.temp values ('Teradex_Mapper');
insert into stats.temp values ('TerrawizBot');
insert into stats.temp values ('TheSuBot');
insert into stats.temp values ('thumbshots-de');
insert into stats.temp values ('TJG/Spider');
insert into stats.temp values ('Tkensaku');
insert into stats.temp values ('Topodia');
insert into stats.temp values ('Toutatis');
insert into stats.temp values ('Traazibot');
insert into stats.temp values ('Trampelpfad');
insert into stats.temp values ('Tumblr');
insert into stats.temp values ('Turnitin');
insert into stats.temp values ('TurnitinBot');
insert into stats.temp values ('TutorGig');
insert into stats.temp values ('Tutorial Crawler');
insert into stats.temp values ('Tv<nn>_Merc_resh_26_1_D-1.0');
insert into stats.temp values ('twiceler');
insert into stats.temp values ('Tygo');
insert into stats.temp values ('TygoBot');
insert into stats.temp values ('UCmore');
insert into stats.temp values ('UdmSearch');
insert into stats.temp values ('UK Searcher Spider');
insert into stats.temp values ('UKWizz');
insert into stats.temp values ('Ultraseek');
insert into stats.temp values ('Updated');
insert into stats.temp values ('updated/0.1beta');
insert into stats.temp values ('UptimeBot');
insert into stats.temp values ('URLBlaze');
insert into stats.temp values ('URL_Spider_Pro');
insert into stats.temp values ('USyd-NLP-Spider');
insert into stats.temp values ('Vagabondo');
insert into stats.temp values ('Vakes');
insert into stats.temp values ('VeryGoodSearch');
insert into stats.temp values ('verzamelgids.nl');
insert into stats.temp values ('Vespa Crawler');
insert into stats.temp values ('Visbot');
insert into stats.temp values ('VisBot');
insert into stats.temp values ('VMBot');
insert into stats.temp values ('Voyager');
insert into stats.temp values ('VSE/1.0');
insert into stats.temp values ('vspider');
insert into stats.temp values ('Vspider');
insert into stats.temp values ('W3SiteSearch');
insert into stats.temp values ('Wavefire');
insert into stats.temp values ('Waypath');
insert into stats.temp values ('WebAlta');
insert into stats.temp values ('WebarooBot');
insert into stats.temp values ('Webclipping.com');
insert into stats.temp values ('webcollage');
insert into stats.temp values ('WebCorp');
insert into stats.temp values ('webcrawl.net');
insert into stats.temp values ('WebFindBot');
insert into stats.temp values ('Weblog Attitude Diffusion');
insert into stats.temp values ('webmeasurement-bot, http://rvs.informatik.uni-leipzig.de');
insert into stats.temp values ('WebRankSpider');
insert into stats.temp values ('WebSearch.COM.AU');
insert into stats.temp values ('WebsiteWorth');
insert into stats.temp values ('Webspinne');
insert into stats.temp values ('Websquash.com');
insert into stats.temp values ('Webverzeichnis.de');
insert into stats.temp values ('whatuseek');
insert into stats.temp values ('whatUseek');
insert into stats.temp values ('whatUseek_winona/3.0');
insert into stats.temp values ('WhizBang! Lab');
insert into stats.temp values ('Willow Internet Crawler by Twotrees');
insert into stats.temp values ('WinkBot');
insert into stats.temp values ('wisenutbot');
insert into stats.temp values ('Worio');
insert into stats.temp values ('Woriobot');
insert into stats.temp values ('WorldLight');
insert into stats.temp values ('Wotbox');
insert into stats.temp values ('wume_crawler');
insert into stats.temp values ('www.arianna.it');
insert into stats.temp values ('WWWeasel');
insert into stats.temp values ('www.inktomisearch.com');
insert into stats.temp values ('www.WebWombat.com.au');
insert into stats.temp values ('X-Crawler');
insert into stats.temp values ('xirq/0.1-beta');
insert into stats.temp values ('xyro_(xcrawler@cosmos.inria.fr)');
insert into stats.temp values ('Yacy');
insert into stats.temp values ('Yahoo');
insert into stats.temp values ('Yahoo-Blogs/v3.9');
insert into stats.temp values ('YahooSeeker/CafeKelsa');
insert into stats.temp values ('Yandex');
insert into stats.temp values ('Y!J');
insert into stats.temp values ('YodaoBot');
insert into stats.temp values ('Yoogli');
insert into stats.temp values ('Yoono');
insert into stats.temp values ('ZACATEK ');
insert into stats.temp values ('Zearchit');
insert into stats.temp values ('Zerxbot');
insert into stats.temp values ('Zeusbot');
insert into stats.temp values ('Zspider');
insert into stats.temp values ('ZyBorg');
insert into stats.temp values ('agadine/1.');
insert into stats.temp values ('crawler@');
insert into stats.temp values ('Crawler V 0.2');
insert into stats.temp values ('Iltrovatore-Setaccio/');
insert into stats.temp values ('larbin_2');
insert into stats.temp values ('moget/');
insert into stats.temp values ('MSRBOT');
insert into stats.temp values ('Nokia-WAPToolkit');
insert into stats.temp values ('AnsearchBot');
insert into stats.temp values ('VSynCrawler');
insert into stats.temp values ('envolk');
insert into stats.temp values ('favorstarbot');
insert into stats.temp values ('webwombat');
insert into stats.temp values ('wectar');
insert into stats.temp values ('GeoBot');
insert into stats.temp values ('wbdbot');
insert into stats.temp values ('SpiritWalker');
insert into stats.temp values ('FDSE robot');
insert into stats.temp values ('FlickBot');
insert into stats.temp values ('planethosting.com');
insert into stats.temp values ('LinkWalker');
insert into stats.temp values ('SpiderKU');
insert into stats.temp values ('InternetLink');
insert into stats.temp values ('OnetSzukaj');
insert into stats.temp values ('webyield');
insert into stats.temp values ('Green Research');
insert into stats.temp values ('PhpDig');
insert into stats.temp values ('GregBot');
insert into stats.temp values ('tuezilla');
insert into stats.temp values ('FasyBug');
insert into stats.temp values ('FastBug');
insert into stats.temp values ('Xenu');
insert into stats.temp values ('NutchCVS');
insert into stats.temp values ('Big B');
insert into stats.temp values ('Links SQL');
insert into stats.temp values ('Html Link');
insert into stats.temp values ('FunnelBack');
insert into stats.temp values ('UIowaCrawler');
insert into stats.temp values ('TsWebBot');
insert into stats.temp values ('iaskspider');
insert into stats.temp values ('NimbleCrawler');
insert into stats.temp values ('battlebot');
insert into stats.temp values ('australian1.com');
insert into stats.temp values ('lwp-trivial');
insert into stats.temp values ('lwp-request');
insert into stats.temp values ('LWP::Simple');
insert into stats.temp values ('snap.com');
insert into stats.temp values ('Infoseek');
insert into stats.temp values ('MouseBOT');
insert into stats.temp values ('Microsoft-ATL-Native');
insert into stats.temp values ('parabot');
insert into stats.temp values ('NetAnts');
insert into stats.temp values ('Teradex_Crawler');
insert into stats.temp values ('Turnitin');
insert into stats.temp values ('findlinks');
insert into stats.temp values ('sherlock_spider');
insert into stats.temp values ('timboBot');
insert into stats.temp values ('VoilaBot');
insert into stats.temp values ('ciml.co.uk');
insert into stats.temp values ('JumbleBot');
insert into stats.temp values ('void-bot');
insert into stats.temp values ('Httpcheck');
insert into stats.temp values ('TulipChain');
insert into stats.temp values ('kuloko-bot');
insert into stats.temp values ('pandora');
insert into stats.temp values ('Search Agent');
insert into stats.temp values ('Netcraft');
insert into stats.temp values ('JoBo/');
insert into stats.temp values ('EmailSiphon');
insert into stats.temp values ('Custo 2.0');
insert into stats.temp values ('EmailSmartz');
insert into stats.temp values ('libwww');
insert into stats.temp values ('WebFilter');
insert into stats.temp values ('RPT-HTTP');
insert into stats.temp values ('BurstFind');
insert into stats.temp values ('DNSGroup');
insert into stats.temp values ('Crawl_App');
insert into stats.temp values ('Gulper Web Bot');
insert into stats.temp values ('DTAAgent');
insert into stats.temp values ('NetNose');
insert into stats.temp values ('Vivante Link');
insert into stats.temp values ('AvantGo');
insert into stats.temp values ('hl_ftien_spider');
insert into stats.temp values ('Art-Online');
insert into stats.temp values ('FusionBot');
insert into stats.temp values ('Zao/');
insert into stats.temp values ('zerxbot');
insert into stats.temp values ('SuperBot');
insert into stats.temp values ('Webinator');
insert into stats.temp values ('WebRACE');
insert into stats.temp values ('voyager/');
insert into stats.temp values ('k2spider');
insert into stats.temp values ('MetaGer-Link');
insert into stats.temp values ('NuSearch');
insert into stats.temp values ('GirafaBot');
insert into stats.temp values ('HooWWWer');
insert into stats.temp values ('TranSGeniKBot');
insert into stats.temp values ('Linkbot');
insert into stats.temp values ('Cerberian');
insert into stats.temp values ('LARBIN');
insert into stats.temp values ('Larbin');
insert into stats.temp values ('W3C-');
insert into stats.temp values ('antibot');
insert into stats.temp values ('BrailleBo');
insert into stats.temp values ('webbot');
insert into stats.temp values ('SYCLIK');
insert into stats.temp values ('BorderManager');
insert into stats.temp values ('SurveyBot');
insert into stats.temp values ('sdcresearch');
insert into stats.temp values ('Teradex');
insert into stats.temp values ('iSEEKbot');
insert into stats.temp values ('Quantcastbot');
insert into stats.temp values ('oso.octopodus');
insert into stats.temp values ('MileNSbot');
insert into stats.temp values ('Snappy');
insert into stats.temp values ('exooba');
insert into stats.temp values ('FeedHub');
insert into stats.temp values ('iFeed');
insert into stats.temp values ('Google/');

alter table stats.agent_staging add column spider boolean DEFAULT false;
alter table stats.agent_staging add column keepwatched boolean DEFAULT false;
