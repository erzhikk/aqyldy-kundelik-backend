-- Backup important data before dropping database

-- Users
COPY (SELECT * FROM app_user) TO STDOUT WITH CSV HEADER;

-- Subjects
COPY (SELECT * FROM subject) TO STDOUT WITH CSV HEADER;

-- Class Levels
COPY (SELECT * FROM class_level) TO STDOUT WITH CSV HEADER;

-- School Classes
COPY (SELECT * FROM school_class) TO STDOUT WITH CSV HEADER;

-- Timetable
COPY (SELECT * FROM timetable) TO STDOUT WITH CSV HEADER;

-- Attendance
COPY (SELECT * FROM attendance) TO STDOUT WITH CSV HEADER;

-- Media Objects
COPY (SELECT * FROM media_object) TO STDOUT WITH CSV HEADER;
