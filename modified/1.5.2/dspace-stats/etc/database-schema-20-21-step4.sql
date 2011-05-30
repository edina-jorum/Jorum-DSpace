CREATE OR REPLACE FUNCTION stats.process_words(integer, text)
  RETURNS integer AS
$BODY$
DECLARE
   nID ALIAS FOR $1;
   nQuery ALIAS FOR $2;

   fID integer;
   fQuery text;
   fQueryHead text;
   fQueryTail text;
   ret int4;
   
   pos int4;
BEGIN
   fQuery := nQuery;
   fID := nID;

   fQuery := trim(lower(fQuery));

   pos := strpos(fQuery,' ');

   if (pos > 0) then
      fQueryHead := split_part(fQuery,' ',1);
      fQueryTail := substr(fQuery,strpos(fQuery,' '));

      ret := stats.process_words(fID,fQueryHead);
      ret := stats.process_words(fID,fQueryTail);
   else
      if length(fQuery) > 3 then
          insert into stats.search_words 
	     (search_words_id, search_id, word)
          values
             (getnextid('stats.search_words'), fID, substr(fQuery, 1, 64));
      end if;
   end if;
   
  RETURN 0;
END;
$BODY$
  LANGUAGE 'plpgsql' VOLATILE;
  
CREATE OR REPLACE FUNCTION stats.migrate_search_words()
  RETURNS integer AS
$BODY$
DECLARE
   records record;
   fQuery text;
   ret integer;
BEGIN
   
   for records in select search_id, query
                  from stats.search
   loop

      fQuery := records.query;

      fQuery := replace(fQuery,'(',' ');
      fQuery := replace(fQuery,')',' ');
      fQuery := replace(fQuery,'"',' ');
      fQuery := replace(fQuery,',',' ');
      fQuery := replace(fQuery,';',' ');
      fQuery := replace(fQuery,'.',' ');
      fQuery := replace(fQuery,'author:','');
      fQuery := replace(fQuery,'title:','');
      fQuery := replace(fQuery,'keyword:','');
      fQuery := replace(fQuery,'abstract:','');
      fQuery := replace(fQuery,'series:','');
      fQuery := replace(fQuery,'sponsor:','');
      fQuery := replace(fQuery,'identifier:','');
      fQuery := replace(fQuery,'language:','');
      fQuery := replace(fQuery,' AND ','');
      fQuery := replace(fQuery,' OR ','');
      fQuery := replace(fQuery,' NOT ','');

      fQuery := trim(lower(fQuery));

      ret := stats.process_words(records.search_id, fQuery);
   end loop;

   return 0;
END;
$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

select stats.migrate_search_words() as result;

drop function stats.migrate_search_words();
drop function stats.process_words(integer, text);

