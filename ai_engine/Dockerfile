FROM python:3.11-slim

WORKDIR /app

# Force TensorFlow to use legacy Keras 2 (supports SavedModel .pb format)
ENV TF_USE_LEGACY_KERAS=1

# TensorFlow and ML dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
