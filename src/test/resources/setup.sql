CREATE TABLE groups (
    grp_id integer NOT NULL generated always as identity(start with 1, increment by 1) primary key,
    grp_name character varying(80) NOT NULL,
--for future, if we want to add support for parentgroup entitlements
    grp_parent integer
);

CREATE TABLE grp_membership (
    id integer NOT NULL generated always as identity(start with 1, increment by 1) primary key,
    grp_id integer,
    usr_id integer,
    membership_start date DEFAULT CURRENT_DATE,
    membership_end date
);

CREATE TABLE poszt (
    id integer NOT NULL generated always as identity(start with 1, increment by 1) primary key,
    grp_member_id integer,
    pttip_id integer
);

CREATE TABLE poszttipus (
    pttip_id integer NOT NULL generated always as identity(start with 1, increment by 1) primary key,
    grp_id integer,
    pttip_name character varying(30) NOT NULL
--there is no boolean type, but we currently don't care about delegated posts
    --delegated_post boolean DEFAULT false
);

CREATE TABLE users (
    usr_id integer NOT NULL generated always as identity(start with 1, increment by 1) primary key,
    usr_neptun char(6)
);
