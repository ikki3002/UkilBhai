FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application files
COPY . .

# Hugging Face Spaces uses port 7860
EXPOSE 7860
ENV PORT=7860
ENV HOST=0.0.0.0

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "7860"]
