import requests
import json

# Configuration
AZURE_ENDPOINT = "https://DeepSeek-R1-gADK.eastus.models.ai.azure.com"
AZURE_API_KEY = "sczzACCarm4XtyfSQz5GQ3v5Hc2hSB2i"
MODEL_NAME = "DeepSeek-R1"  # Try with the model name that worked in testing
API_VERSION = "2024-05-01-preview"

# First, let's check if we can access the base URL
print(f"Testing connection to {AZURE_ENDPOINT}...")
try:
    response = requests.get(AZURE_ENDPOINT)
    print(f"Status code: {response.status_code}")
    print(f"Response: {response.text[:200]}...")
except Exception as e:
    print(f"Error: {e}")

# Let's try listing deployments
print("\nTrying to list deployments...")
deployments_url = f"{AZURE_ENDPOINT}/openai/deployments?api-version={API_VERSION}"
headers = {"Authorization": f"Bearer {AZURE_API_KEY}"}
try:
    response = requests.get(deployments_url, headers=headers)
    print(f"Status code: {response.status_code}")
    print(f"Response: {response.text[:200]}...")
except Exception as e:
    print(f"Error: {e}")

# Now let's try a chat completion
print("\nTrying chat completion...")
chat_url = f"{AZURE_ENDPOINT}/openai/deployments/{MODEL_NAME}/chat/completions?api-version={API_VERSION}"
headers = {
    "Authorization": f"Bearer {AZURE_API_KEY}", 
    "Content-Type": "application/json"
}
data = {
    "messages": [
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "Hello, can you help me learn Java?"}
    ],
    "max_tokens": 100,
    "temperature": 0.7
}

try:
    response = requests.post(chat_url, headers=headers, json=data, timeout=10)
    print(f"Status code: {response.status_code}")
    print(f"Response: {response.text[:500]}...")
except Exception as e:
    print(f"Error: {e}")

print("\nTest complete.") 