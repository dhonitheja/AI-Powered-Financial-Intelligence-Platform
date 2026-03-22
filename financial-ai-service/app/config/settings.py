from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional

class Settings(BaseSettings):
    gemini_api_key: Optional[str] = None
    model_name: str = "gemini-1.5-flash"
    
    log_level: str = "info"

    # Service-to-service shared secret: Spring Boot -> AI Service
    # Must be set in both services' environment variables
    internal_ai_service_secret: Optional[str] = None

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding='utf-8', extra='ignore')

settings = Settings()
print(f"DEBUG: Loaded GEMINI_API_KEY: {settings.gemini_api_key[:8]}..." if settings.gemini_api_key else "DEBUG: GEMINI_API_KEY IS NONE")
