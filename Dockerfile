FROM python:3.6-alpine3.6

ARG BUILD_DATE="1970-01-01"
ARG VCS_REF="n/a"

LABEL org.opencontainers.image.authors="nkijak@gmail.com"
LABEL build.date=$BUILD_DATE
LABEL build.ref=$VCS_REF

WORKDIR /usr/src/app

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt


COPY lib ./lib
COPY couchrepo ./couchrepo
COPY nypTracks.py .
COPY nyp_track_events.py .
VOLUME /usr/src/app/dv_data

CMD ["python", "nypTracks.py"]

