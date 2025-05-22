import requests
import json

# Using the exact configuration provided
AZURE_ENDPOINT = "https://DeepSeek-R1-gADK.eastus.models.ai.azure.com"
AZURE_API_KEY = "sczzACCarm4XtyfSQz5GQ3v5Hc2hSB2i"
MODEL_NAME = "DeepSeek-R1-gADK"
API_VERSION = "2024-06-01-preview"

# Exact URL provided
url = f"{AZURE_ENDPOINT}/v1/chat/completions?api-version={API_VERSION}"

print(f"Testing with exact configuration:")
print(f"URL: {url}")
print(f"Model: {MODEL_NAME}")
print(f"API Version: {API_VERSION}")

# Setting up the request
headers = {
    "Authorization": f"Bearer {AZURE_API_KEY}",
    "Content-Type": "application/json"
}

data = {
    "model": MODEL_NAME,
    "messages": [
        {"role": "system", "content": "You are a helpful learning assistant for an e-learning app called SkillLearn."},
        {"role": "user", "content": "Hello, can you help me learn Java?"}
    ],
    "max_tokens": 100,
    "temperature": 0.7
}

# Make the request
try:
    print("\nSending request...")
    response = requests.post(url, headers=headers, json=data, timeout=20)
    print(f"Status code: {response.status_code}")
    
    # Print response
    if response.status_code == 200:
        print("SUCCESS!")
        try:
            json_response = response.json()
            content = json_response["choices"][0]["message"]["content"]
            print(f"\nResponse content: {content}")
        except Exception as e:
            print(f"Error parsing response: {e}")
            print(f"Raw response: {response.text[:500]}...")
    else:
        print(f"Error response: {response.text[:500]}...")
except Exception as e:
    print(f"Request error: {e}")

# Try alternate format without model in body
print("\n\nTrying alternate format (without model in body)...")
data.pop("model", None)  # Remove model from request body

try:
    print("Sending request...")
    response = requests.post(url, headers=headers, json=data, timeout=20)
    print(f"Status code: {response.status_code}")
    
    # Print response
    if response.status_code == 200:
        print("SUCCESS!")
        try:
            json_response = response.json()
            content = json_response["choices"][0]["message"]["content"]
            print(f"\nResponse content: {content}")
        except Exception as e:
            print(f"Error parsing response: {e}")
            print(f"Raw response: {response.text[:500]}...")
    else:
        print(f"Error response: {response.text[:500]}...")
except Exception as e:
    print(f"Request error: {e}")

print("\nTest complete.") 