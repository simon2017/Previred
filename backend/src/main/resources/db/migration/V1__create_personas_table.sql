CREATE TABLE personas
(
    rut              VARCHAR(12)  PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL,
    apellido         VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE         NOT NULL,
    calle            VARCHAR(150) NOT NULL,
    comuna           VARCHAR(100) NOT NULL,
    region           VARCHAR(100) NOT NULL,
    created_at       DATETIMEOFFSET(6) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    updated_at       DATETIMEOFFSET(6) NOT NULL DEFAULT SYSDATETIMEOFFSET()
);
