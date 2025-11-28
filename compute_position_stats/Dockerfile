FROM python:3.14.0

COPY . /app

RUN pip install --no-cache-dir psycopg2-binary python-chess mpi4py impi-rt python-dotenv

ENTRYPOINT ["python", "main.py"]