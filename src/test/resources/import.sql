--groups
INSERT INTO groups(grp_name, grp_parent) VALUES ('KIR fejlesztők és üzemeltetők', NULL);
INSERT INTO groups(grp_name, grp_parent) VALUES ('SVIE', NULL);
INSERT INTO groups(grp_name, grp_parent) VALUES ('Pizzásch', NULL);
INSERT INTO groups(grp_name, grp_parent) VALUES ('Gofffree', NULL);
INSERT INTO groups(grp_name, grp_parent) VALUES ('La''Place', 2);

--poszttipus
INSERT INTO poszttipus (grp_id, pttip_name) VALUES(NULL,'gazdaságis');
INSERT INTO poszttipus (grp_id, pttip_name) VALUES(NULL,'PR menedzser');
INSERT INTO poszttipus (grp_id, pttip_name) VALUES(NULL,'körvezető');
INSERT INTO poszttipus (grp_id, pttip_name) VALUES(NULL,'volt körvezető');
INSERT INTO poszttipus (grp_id, pttip_name) VALUES(NULL,'vendégfogadó');
INSERT INTO poszttipus (grp_id, pttip_name) VALUES(NULL,'feldolgozás alatt');
INSERT INTO poszttipus (grp_id, pttip_name) VALUES(1,'választmányi elnök');
INSERT INTO poszttipus (grp_id, pttip_name) VALUES(2,'tanfolyamfelelős');

--Nincs tagság
INSERT INTO users (usr_neptun) VALUES('ABCDEF');

--Aktív tag Kir-Dev-ben
INSERT INTO users (usr_neptun) VALUES('GHIJKL');
INSERT INTO grp_membership (grp_id, usr_id, membership_start, membership_end)
VALUES (1,2,'2010-01-01', NULL);

--Öregtag egy körben
INSERT INTO users (usr_neptun) VALUES('MNOPQR');
INSERT INTO grp_membership (grp_id, usr_id, membership_start, membership_end)
VALUES (2,3,'2010-01-01', '2010-10-12');


--Pizzásch körvezető
INSERT INTO users (usr_neptun) VALUES('STUVWX');
INSERT INTO grp_membership (grp_id, usr_id, membership_start, membership_end)
VALUES (3,4,'2010-01-01', NULL);
INSERT INTO poszt (grp_member_id, pttip_id) VALUES (3, 3);

--Kir-Dev feldolgozás alatt
INSERT INTO users (usr_neptun) VALUES('YZABCD');
INSERT INTO grp_membership (grp_id, usr_id, membership_start, membership_end)
VALUES (1,5,'2010-01-01', NULL);
INSERT INTO poszt (grp_member_id, pttip_id) VALUES (4, 6);

--La'Place gazdaságis és La'Place PR menedzser
INSERT INTO users (usr_neptun) VALUES('EFGHIJ');
INSERT INTO grp_membership (grp_id, usr_id, membership_start, membership_end)
VALUES (5,6,'2010-01-01', NULL);
INSERT INTO poszt (grp_member_id, pttip_id) VALUES (5, 1);
INSERT INTO poszt (grp_member_id, pttip_id) VALUES (5, 2);

--Gofffree és Pizzásch tag
INSERT INTO users (usr_neptun) VALUES('KLMNOP');
INSERT INTO grp_membership (grp_id, usr_id, membership_start, membership_end)
VALUES (4,7,'2010-01-01', NULL);
INSERT INTO grp_membership (grp_id, usr_id, membership_start, membership_end)
VALUES (3,7,'2010-01-01', NULL);

--La'Place tag, SVIE volt körvezető
INSERT INTO users (usr_neptun) VALUES('QRSTUV');
INSERT INTO grp_membership (grp_id, usr_id, membership_start, membership_end)
VALUES (5,8,'2010-01-01', NULL);
INSERT INTO grp_membership (grp_id, usr_id, membership_start, membership_end)
VALUES (2,8,'2010-01-01', NULL);
INSERT INTO poszt (grp_member_id, pttip_id) VALUES (9, 4);

--Gofffree tag, SVIE körvezető, Kir-Dev öregtag (volt körvezető közben)
INSERT INTO users (usr_neptun) VALUES('XYZABC');
INSERT INTO grp_membership (grp_id, usr_id, membership_start, membership_end)
VALUES (1,9,'2010-01-01', '2010-10-31');
INSERT INTO grp_membership (grp_id, usr_id, membership_start, membership_end)
VALUES (4,9,'2010-01-01', NULL);
INSERT INTO grp_membership (grp_id, usr_id, membership_start, membership_end)
VALUES (2,9,'2010-01-01', NULL);
INSERT INTO poszt (grp_member_id, pttip_id) VALUES (10, 4);
--SVIE körvezető
INSERT INTO poszt (grp_member_id, pttip_id) VALUES (12, 3);


