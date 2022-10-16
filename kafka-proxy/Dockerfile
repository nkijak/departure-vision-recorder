FROM python:3.6-alpine


WORKDIR /root/

COPY requirements.txt   .
RUN pip install -r requirements.txt
COPY index.py           .

ENV fprocess="python index.py"

CMD ["python", "index.py"]