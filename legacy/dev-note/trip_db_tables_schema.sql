--
-- PostgreSQL database dump
--

\restrict XsJQvfTSsjhnMPbPcOvVEb3QgyjtoTgX0Lg0bGGRRYjV8DySoeX278N5y5fadaC

-- Dumped from database version 15.15 (Debian 15.15-1.pgdg13+1)
-- Dumped by pg_dump version 15.15 (Debian 15.15-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: rating; Type: TABLE; Schema: public; Owner: trip_service_user
--

CREATE TABLE public.rating (
    id uuid NOT NULL,
    comment character varying(500),
    created_at timestamp(6) without time zone NOT NULL,
    score integer NOT NULL,
    trip_id uuid NOT NULL
);


ALTER TABLE public.rating OWNER TO trip_service_user;

--
-- Name: trip; Type: TABLE; Schema: public; Owner: trip_service_user
--

CREATE TABLE public.trip (
    id uuid NOT NULL,
    cancelled_at timestamp(6) without time zone,
    completed_at timestamp(6) without time zone,
    destination_latitude double precision NOT NULL,
    destination_longitude double precision NOT NULL,
    driver_id uuid,
    fare numeric(38,2),
    passenger_id uuid NOT NULL,
    pickup_latitude double precision NOT NULL,
    pickup_longitude double precision NOT NULL,
    requested_at timestamp(6) without time zone NOT NULL,
    started_at timestamp(6) without time zone,
    status character varying(255) NOT NULL,
    CONSTRAINT trip_status_check CHECK (((status)::text = ANY ((ARRAY['SEARCHING_DRIVER'::character varying, 'ACCEPTED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);


ALTER TABLE public.trip OWNER TO trip_service_user;

--
-- Name: rating rating_pkey; Type: CONSTRAINT; Schema: public; Owner: trip_service_user
--

ALTER TABLE ONLY public.rating
    ADD CONSTRAINT rating_pkey PRIMARY KEY (id);


--
-- Name: trip trip_pkey; Type: CONSTRAINT; Schema: public; Owner: trip_service_user
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_pkey PRIMARY KEY (id);


--
-- Name: rating uk9w94ijxamvg5h8a92x0yp85so; Type: CONSTRAINT; Schema: public; Owner: trip_service_user
--

ALTER TABLE ONLY public.rating
    ADD CONSTRAINT uk9w94ijxamvg5h8a92x0yp85so UNIQUE (trip_id);


--
-- Name: rating fkh9iqu0kywm9n2aobfh7651r4x; Type: FK CONSTRAINT; Schema: public; Owner: trip_service_user
--

ALTER TABLE ONLY public.rating
    ADD CONSTRAINT fkh9iqu0kywm9n2aobfh7651r4x FOREIGN KEY (trip_id) REFERENCES public.trip(id);


--
-- PostgreSQL database dump complete
--

\unrestrict XsJQvfTSsjhnMPbPcOvVEb3QgyjtoTgX0Lg0bGGRRYjV8DySoeX278N5y5fadaC

