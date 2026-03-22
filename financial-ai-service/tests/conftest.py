import pytest

def pytest_configure(config):
    config.addinivalue_line(
        "markers", "asyncio: mark test as requiring asyncio"
    )

@pytest.fixture(scope="session")
def anyio_backend():
    return "asyncio"
