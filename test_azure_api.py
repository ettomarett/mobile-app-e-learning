import requests
import json

# API Configuration
AZURE_ENDPOINT = "https://DeepSeek-R1-gADK.eastus.models.ai.azure.com"
AZURE_API_KEY = "sczzACCarm4XtyfSQz5GQ3v5Hc2hSB2i"
AZURE_MODEL_NAME = "DeepSeek-R1-gADK"  # Try with and without the deployment name
API_VERSION = "2024-05-01-preview"  # This is the latest version

def test_endpoint(url_format, model_name):
    """Test different API endpoint formats"""
    url = url_format.format(
        endpoint=AZURE_ENDPOINT.rstrip('/'),
        model=model_name,
        api_version=API_VERSION
    )
    
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {AZURE_API_KEY}"
    }
    
    data = {
        "messages": [
            {
                "role": "system",
                "content": "You are a helpful learning assistant for an e-learning app called SkillLearn."
            },
            {
                "role": "user",
                "content": "Hello, can you help me learn Java?"
            }
        ],
        "max_tokens": 800,
        "temperature": 0.7
    }
    
    print(f"\nTesting URL: {url}")
    try:
        response = requests.post(url, headers=headers, json=data)
        print(f"Status code: {response.status_code}")
        
        if response.status_code == 200:
            json_response = response.json()
            content = json_response["choices"][0]["message"]["content"]
            print("Success! Assistant's Response:")
            print(content)
            return True
        else:
            print(f"Error Response: {response.text}")
            return False
    except Exception as e:
        print(f"Error: {e}")
        return False

# Test different URL formats and model names
url_formats = [
    # Standard format with deployment name
    "{endpoint}/openai/deployments/{model}/chat/completions?api-version={api_version}",
    
    # Try without 'openai/' prefix
    "{endpoint}/deployments/{model}/chat/completions?api-version={api_version}",
    
    # Try with different capitalization
    "{endpoint}/openai/deployments/{model}/chat/completions?api-version={api_version}",
    
    # Try with api_key as a query parameter instead of header
    "{endpoint}/openai/deployments/{model}/chat/completions?api-version={api_version}&api_key={api_key}"
]

model_variants = [
    "DeepSeek-R1-gADK",  # Original
    "deepseek-r1-gadk",  # Lowercase
    "DeepSeek-R1",       # Without suffix
    "DeepSeekR1"         # Without hyphens
]

print("Testing various Azure OpenAI API endpoint formats...")
success = False

for url_format in url_formats:
    if "{api_key}" in url_format:
        # Special case for URL with API key in query
        url = url_format.format(
            endpoint=AZURE_ENDPOINT.rstrip('/'),
            model=AZURE_MODEL_NAME,
            api_version=API_VERSION,
            api_key=AZURE_API_KEY
        )
        
        headers = {"Content-Type": "application/json"}
        
        data = {
            "messages": [
                {"role": "system", "content": "You are a helpful assistant."},
                {"role": "user", "content": "Hello, can you help me learn Java?"}
            ],
            "max_tokens": 800,
            "temperature": 0.7
        }
        
        print(f"\nTesting URL with API key in query: {url}")
        try:
            response = requests.post(url, headers=headers, json=data)
            print(f"Status code: {response.status_code}")
            if response.status_code == 200:
                success = True
                break
            else:
                print(f"Error Response: {response.text}")
        except Exception as e:
            print(f"Error: {e}")
    else:
        # Test different model names with this URL format
        for model in model_variants:
            if test_endpoint(url_format, model):
                success = True
                break
    
    if success:
        break

if not success:
    print("\nAll endpoint formats failed. Please check your Azure OpenAI configuration.")
    # Let's also try the API version suggested by the error
    print("\nTrying with API version 2023-05-15...")
    test_endpoint("{endpoint}/openai/deployments/{model}/chat/completions?api-version=2023-05-15", AZURE_MODEL_NAME) 