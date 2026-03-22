# Use a slim Python image for speed
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install system dependencies for some AI libraries and health checks
RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user for security and its home dir
RUN groupadd -r wealthix && useradd -mr -g wealthix wealthix
WORKDIR /app
RUN mkdir -p /home/wealthix/.config/gcloud && chown -R wealthix:wealthix /home/wealthix

# Copy requirements and install
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Ensure the app directory is owned by the non-root user
RUN chown -R wealthix:wealthix /app

# Switch to non-root user
USER wealthix

# Expose FastAPI port
EXPOSE 8000

# Health check to ensure the router is ready
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s \
    --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# Run the application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
