--liquibase formatted sql

--changeset system:002-insert-initial-appointments
INSERT INTO appointment (title) VALUES ('Doctor Appointment');
INSERT INTO appointment (title) VALUES ('Dentist Visit');
INSERT INTO appointment (title) VALUES ('Business Meeting');
INSERT INTO appointment (title) VALUES ('Gym Session');
INSERT INTO appointment (title) VALUES ('Mechanic Appointment');
INSERT INTO appointment (title) VALUES ('Lawer Appointment');
INSERT INTO appointment (title) VALUES ('Tax Adviser Consultation');
INSERT INTO appointment (title) VALUES ('G20 Davos Summit');
