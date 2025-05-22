import requests

# API Configuration
AZURE_ENDPOINT = "https://DeepSeek-R1-gADK.eastus.models.ai.azure.com"
AZURE_API_KEY = "sczzACCarm4XtyfSQz5GQ3v5Hc2hSB2i"

# Try to access the base URL
try:
    print(f"Checking if base URL is accessible: {AZURE_ENDPOINT}")
    response = requests.get(AZURE_ENDPOINT)
    print(f"Status code: {response.status_code}")
    print(f"Response headers: {response.headers}")
    print(f"Response: {response.text[:200]}...")  # Print first 200 chars
except Exception as e:
    print(f"Error accessing base URL: {e}")

# Try with authorization header
try:
    print("\nChecking with authorization header...")
    headers = {"Authorization": f"Bearer {AZURE_API_KEY}"}
    response = requests.get(AZURE_ENDPOINT, headers=headers)
    print(f"Status code: {response.status_code}")
    print(f"Response headers: {response.headers}")
    print(f"Response: {response.text[:200]}...")  # Print first 200 chars
except Exception as e:
    print(f"Error accessing with auth: {e}")

# Try Azure OpenAI's health check endpoints if they exist
health_endpoints = [
    "/health",
    "/openai/health",
    "/status",
    "/openai/status",
    "/ready",
    "/openai/ready"
]

for endpoint in health_endpoints:
    url = AZURE_ENDPOINT.rstrip('/') + endpoint
    try:
        print(f"\nChecking health endpoint: {url}")
        response = requests.get(url)
        print(f"Status code: {response.status_code}")
        if response.status_code < 400:  # Success
            print(f"Response: {response.text[:200]}...")
    except Exception as e:
        print(f"Error: {e}")

print("\nNote: 404 errors are expected for non-existent endpoints, but this shows the server is reachable.") 