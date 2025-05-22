import requests
import json

# API Configuration
AZURE_ENDPOINT = "https://DeepSeek-R1-gADK.eastus.models.ai.azure.com"
AZURE_API_KEY = "sczzACCarm4XtyfSQz5GQ3v5Hc2hSB2i"
AZURE_MODEL_NAME = "DeepSeek-R1-gADK"
API_VERSION = "2024-05-01-preview"

# Try official Azure OpenAI format with api-key header
url = f"{AZURE_ENDPOINT}/openai/deployments/{AZURE_MODEL_NAME}/chat/completions?api-version={API_VERSION}"

# Different header combinations to try
header_variants = [
    # Official Azure OpenAI API format
    {
        "Content-Type": "application/json",
        "api-key": AZURE_API_KEY
    },
    # Try with different auth formats
    {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {AZURE_API_KEY}"
    },
    {
        "Content-Type": "application/json",
        "api-key": AZURE_API_KEY,
        "Authorization": f"Bearer {AZURE_API_KEY}"
    }
]

data = {
    "messages": [
        {
            "role": "system",
            "content": "You are a helpful learning assistant."
        },
        {
            "role": "user",
            "content": "Hello, can you help me learn Java?"
        }
    ],
    "max_tokens": 800,
    "temperature": 0.7
}

# Try with different header combinations
for i, headers in enumerate(header_variants):
    print(f"\nTrying header variant {i+1}: {headers}")
    try:
        response = requests.post(url, headers=headers, json=data)
        print(f"Status code: {response.status_code}")
        print(f"Response headers: {response.headers}")
        
        if response.status_code == 200:
            json_response = response.json()
            content = json_response["choices"][0]["message"]["content"]
            print("Success! Assistant's Response:")
            print(content)
        else:
            print(f"Error Response: {response.text}")
    except Exception as e:
        print(f"Error: {e}")

# Also try to list available deployments if possible
list_deployments_url = f"{AZURE_ENDPOINT}/openai/deployments?api-version={API_VERSION}"
print(f"\nTrying to list available deployments: {list_deployments_url}")
try:
    for headers in header_variants:
        print(f"Using headers: {headers}")
        response = requests.get(list_deployments_url, headers=headers)
        print(f"Status code: {response.status_code}")
        if response.status_code == 200:
            print(json.dumps(response.json(), indent=2))
        else:
            print(f"Error: {response.text}")
except Exception as e:
    print(f"Error: {e}")

print("\nNote: If all attempts failed, the most likely causes are:")
print("1. The model deployment name is incorrect")
print("2. The API key is incorrect or expired")
print("3. The Azure OpenAI service URL is incorrect")
print("4. The API version is not compatible with this deployment") 