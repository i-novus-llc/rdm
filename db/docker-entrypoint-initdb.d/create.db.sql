CREATE DATABASE rdm;

\connect rdm
CREATE TEXT SEARCH DICTIONARY ispell_ru (template= ispell,dictfile= ru, afffile = ru);
CREATE TEXT SEARCH CONFIGURATION ru (COPY = russian);
ALTER TEXT SEARCH CONFIGURATION ru ALTER MAPPING FOR word, hword, hword_part WITH ispell_ru, russian_stem;
