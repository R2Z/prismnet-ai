# Use the latest stable MySQL image
FROM mysql:latest

# Set environment variables for MySQL
ENV MYSQL_ROOT_PASSWORD=t!g3rP_k
ENV MYSQL_DATABASE=prismnetai
ENV MYSQL_USER=prismnetai
ENV MYSQL_PASSWORD=t!g3rP_k

# Copy initialization SQL scripts into the official MySQL entrypoint directory.
# Files placed in /docker-entrypoint-initdb.d/ are executed in alphabetical order
# when a fresh database is initialized on container start. To ensure the
# desired order run (kingston.sql then admin.sql) we prefix files with 01_ and
# 02_ respectively.

# Copy all migration SQL scripts from the resources migration directory into
# the MySQL init folder. Files in `/docker-entrypoint-initdb.d/` are executed
# in alphabetical order on first container startup. Keeping the files in
# `src/main/resources/db/migration/` (as in the project) => copy the whole
# directory's SQLs so the build doesn't fail if files are added/renamed.
COPY src/main/resources/db/migration/*.sql /docker-entrypoint-initdb.d/

# Expose MySQL port
EXPOSE 3306

# Use default entrypoint from the MySQL base image; the init scripts above will
# be executed at first container startup to populate the `kingston` database.

# Metadata: logical network this service should be attached to when run via
# docker-compose (Dockerfiles can't force network assignment at build time).
LABEL com.prismnetai.network=prismnetai-network
