INSERT INTO app_users (id,name,email) VALUES (1,'Житель 1','resident1@example.com'),(2,'Житель 2','resident2@example.com');
INSERT INTO premises (id,address,area,owner_id) VALUES (1,'ул. Лесная, д. 5, кв. 12',52.5,1);
INSERT INTO meters (id,premise_id,resource_type,serial_number,tariff,status,created_at)
VALUES (1,1,'COLD_WATER','DEMO-CW-1',45.50,'ACTIVE',date_trunc('month',now())-interval '3 months'),
       (2,1,'ELECTRICITY','DEMO-EL-1',6.00,'ACTIVE',date_trunc('month',now())-interval '3 months');
INSERT INTO meter_readings (id,meter_id,period,value,status) VALUES
 (1,1,to_char(now()-interval '2 months','YYYY-MM'),100,'VERIFIED'),
 (2,1,to_char(now()-interval '1 month','YYYY-MM'),115,'PENDING'),
 (3,2,to_char(now()-interval '2 months','YYYY-MM'),200,'VERIFIED'),
 (4,2,to_char(now()-interval '1 month','YYYY-MM'),230,'PENDING');
SELECT setval(pg_get_serial_sequence('app_users','id'),(SELECT max(id) FROM app_users));
SELECT setval(pg_get_serial_sequence('premises','id'),(SELECT max(id) FROM premises));
SELECT setval(pg_get_serial_sequence('meters','id'),(SELECT max(id) FROM meters));
SELECT setval(pg_get_serial_sequence('meter_readings','id'),(SELECT max(id) FROM meter_readings));
