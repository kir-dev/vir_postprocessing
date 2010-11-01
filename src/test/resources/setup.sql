--
-- Copyright (c) 2008-2010, Peter Major
-- All rights reserved.
--
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are met:
-- * Redistributions of source code must retain the above copyright
-- notice, this list of conditions and the following disclaimer.
--  * Redistributions in binary form must reproduce the above copyright
-- notice, this list of conditions and the following disclaimer in the
-- documentation and/or other materials provided with the distribution.
--  * Neither the name of the Peter Major nor the
-- names of its contributors may be used to endorse or promote products
-- derived from this software without specific prior written permission.
--  * All advertising materials mentioning features or use of this software
-- must display the following acknowledgement:
-- This product includes software developed by the Kir-Dev Team, Hungary
-- and its contributors.
--
-- THIS SOFTWARE IS PROVIDED BY Peter Major ''AS IS'' AND ANY
-- EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
-- WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
-- DISCLAIMED. IN NO EVENT SHALL Peter Major BE LIABLE FOR ANY
-- DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
-- (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
-- LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
-- ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
-- (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
-- SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
--

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
