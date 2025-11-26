FROM python:3.14.0

COPY . /app

RUN pip install --no-cache-dir psycopg2-binary python-chess

ENTRYPOINT ["python", "main.py"]