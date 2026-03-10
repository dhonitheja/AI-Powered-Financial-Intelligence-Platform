from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional

class Settings(BaseSettings):
    gemini_api_key: Optional[str] = None
    model_name: str = "gemini-flash-latest"
    
    log_level: str = "info"

    # Service-to-service shared secret: Spring Boot -> AI Service
    # Must be set in both services' environment variables
    autopay_internal_secret: Optional[str] = None

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding='utf-8')

settings = Settings()
