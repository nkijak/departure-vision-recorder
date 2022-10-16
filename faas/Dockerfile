FROM python:3.6-alpine


# RUN apk --no-cache add musl-dev gcc make openssl-dev libffi-dev

WORKDIR /root/

COPY requirements.txt   .
RUN pip install -r requirements.txt
COPY handler.py           .


CMD ["python", "handler.py"]