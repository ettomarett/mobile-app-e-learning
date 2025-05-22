import os
import requests
import json
from datetime import datetime
import time

# Configuration
AZURE_ENDPOINT = "https://DeepSeek-R1-gADK.eastus.models.ai.azure.com"
AZURE_API_KEY = "sczzACCarm4XtyfSQz5GQ3v5Hc2hSB2i"

# Models to try
MODEL_NAMES = [
    "DeepSeek-R1-gADK",
    "DeepSeek-R1",
    "deepseek-r1",
    "DeepSeek-Coder",
    "deepseek-coder",
    "gpt-35-turbo",     # Common Azure deployments
    "gpt-4",
    "text-davinci-003",
    "text-embedding-ada-002"
]

# API versions to try
API_VERSIONS = [
    "2024-05-01-preview",
    "2023-12-01-preview",
    "2023-09-01-preview",
    "2023-05-15",
    "2023-03-15-preview",
    "2022-12-01"
]

# Create log file
log_filename = f"azure_test_log_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"

def log_message(message):
    """Log a message to both console and file"""
    print(message)
    with open(log_filename, "a", encoding="utf-8") as f:
        f.write(message + "\n")

def test_basic_connection():
    """Test basic connection to the Azure endpoint"""
    log_message("\n===== Testing Basic Connection =====")
    
    try:
        # Try without any auth
        response = requests.get(AZURE_ENDPOINT)
        log_message(f"Basic GET (no auth): Status {response.status_code}")
        log_message(f"Response: {response.text[:200]}...")
    except Exception as e:
        log_message(f"Error with basic connection: {str(e)}")
    
    try:
        # Try with auth header
        headers = {"Authorization": f"Bearer {AZURE_API_KEY}"}
        response = requests.get(AZURE_ENDPOINT, headers=headers)
        log_message(f"Basic GET (with auth): Status {response.status_code}")
        log_message(f"Response: {response.text[:200]}...")
    except Exception as e:
        log_message(f"Error with auth connection: {str(e)}")

def test_list_deployments():
    """Test listing deployments"""
    log_message("\n===== Testing List Deployments =====")
    
    # Auth header variations to try
    auth_headers = [
        {"Authorization": f"Bearer {AZURE_API_KEY}"},
        {"api-key": AZURE_API_KEY},
        {"Authorization": f"Bearer {AZURE_API_KEY}", "api-key": AZURE_API_KEY}
    ]
    
    for api_version in API_VERSIONS:
        url = f"{AZURE_ENDPOINT}/openai/deployments?api-version={api_version}"
        log_message(f"\nTrying to list deployments with API version: {api_version}")
        log_message(f"URL: {url}")
        
        for i, headers in enumerate(auth_headers):
            log_message(f"  Testing auth headers variation {i+1}: {headers}")
            try:
                response = requests.get(url, headers=headers)
                log_message(f"  Status: {response.status_code}")
                log_message(f"  Response: {response.text[:200]}...")
                
                if response.status_code == 200:
                    log_message("SUCCESS! Deployments found.")
                    return True
            except Exception as e:
                log_message(f"  Error: {str(e)}")
    
    return False

def test_chat_completions():
    """Test chat completions with different models and API versions"""
    log_message("\n===== Testing Chat Completions =====")
    
    # Message content
    messages = [
        {"role": "system", "content": "You are a helpful learning assistant for an e-learning app called SkillLearn."},
        {"role": "user", "content": "Hello, can you help me learn Java?"}
    ]
    
    # Request data
    data = {
        "messages": messages,
        "max_tokens": 100,
        "temperature": 0.7
    }
    
    # Headers to try
    auth_headers = [
        {"Authorization": f"Bearer {AZURE_API_KEY}", "Content-Type": "application/json"},
        {"api-key": AZURE_API_KEY, "Content-Type": "application/json"},
        {"Authorization": f"Bearer {AZURE_API_KEY}", "api-key": AZURE_API_KEY, "Content-Type": "application/json"}
    ]
    
    # Try all combinations
    for model_name in MODEL_NAMES:
        for api_version in API_VERSIONS:
            log_message(f"\nTrying model: {model_name}, API version: {api_version}")
            
            # URL pattern variations to try
            url_patterns = [
                f"{AZURE_ENDPOINT}/openai/deployments/{model_name}/chat/completions?api-version={api_version}",
                f"{AZURE_ENDPOINT}/deployments/{model_name}/chat/completions?api-version={api_version}"
            ]
            
            for url in url_patterns:
                log_message(f"  URL: {url}")
                
                for i, headers in enumerate(auth_headers):
                    log_message(f"    Auth headers variation {i+1}")
                    
                    try:
                        response = requests.post(url, headers=headers, json=data, timeout=10)
                        log_message(f"    Status: {response.status_code}")
                        
                        # Get response text, but handle possible errors
                        try:
                            response_text = response.text[:500]
                        except:
                            response_text = "Could not extract response text"
                        
                        log_message(f"    Response: {response_text}...")
                        
                        if response.status_code == 200:
                            log_message("\n!! SUCCESS !!")
                            log_message(f"Working combination found!")
                            log_message(f"Model: {model_name}")
                            log_message(f"API Version: {api_version}")
                            log_message(f"URL: {url}")
                            log_message(f"Headers: {headers}")
                            
                            # Try to extract and print the actual completion
                            try:
                                content = response.json()["choices"][0]["message"]["content"]
                                log_message(f"\nModel Response: {content}")
                            except Exception as e:
                                log_message(f"Error extracting content: {str(e)}")
                                
                            return True
                    except requests.exceptions.Timeout:
                        log_message("    Request timed out after 10 seconds")
                    except Exception as e:
                        log_message(f"    Error: {str(e)}")
                    
                    # Add a small delay to avoid throttling
                    time.sleep(0.5)
    
    return False

def test_completions():
    """Test standard completions endpoint (not chat)"""
    log_message("\n===== Testing Standard Completions =====")
    
    # Request data
    data = {
        "prompt": "Write a Java Hello World program",
        "max_tokens": 100,
        "temperature": 0.7
    }
    
    # Headers to try
    auth_headers = [
        {"Authorization": f"Bearer {AZURE_API_KEY}", "Content-Type": "application/json"},
        {"api-key": AZURE_API_KEY, "Content-Type": "application/json"}
    ]
    
    # Try all combinations
    for model_name in MODEL_NAMES:
        for api_version in API_VERSIONS:
            log_message(f"\nTrying model: {model_name}, API version: {api_version}")
            
            # URL for completions (not chat completions)
            url = f"{AZURE_ENDPOINT}/openai/deployments/{model_name}/completions?api-version={api_version}"
            
            log_message(f"  URL: {url}")
            
            for i, headers in enumerate(auth_headers):
                log_message(f"    Auth headers variation {i+1}")
                
                try:
                    response = requests.post(url, headers=headers, json=data, timeout=10)
                    log_message(f"    Status: {response.status_code}")
                    log_message(f"    Response: {response.text[:200]}...")
                    
                    if response.status_code == 200:
                        log_message("\n!! SUCCESS !!")
                        log_message(f"Working combination found!")
                        log_message(f"Model: {model_name}")
                        log_message(f"API Version: {api_version}")
                        log_message(f"URL: {url}")
                        log_message(f"Headers: {headers}")
                        return True
                except requests.exceptions.Timeout:
                    log_message("    Request timed out after 10 seconds")
                except Exception as e:
                    log_message(f"    Error: {str(e)}")
                
                # Add a small delay to avoid throttling
                time.sleep(0.5)
    
    return False

if __name__ == "__main__":
    log_message("Starting Azure OpenAI API Test")
    log_message(f"Azure Endpoint: {AZURE_ENDPOINT}")
    log_message(f"Log file: {log_filename}")
    
    # Run tests
    test_basic_connection()
    deployment_success = test_list_deployments()
    
    if not deployment_success:
        log_message("\nCould not list deployments. Continuing with chat completions tests...")
    
    chat_success = test_chat_completions()
    
    if not chat_success:
        log_message("\nChat completions failed. Trying standard completions...")
        completions_success = test_completions()
        
        if not completions_success:
            log_message("\nAll tests failed. Please check your Azure OpenAI configuration.")
    
    log_message("\nTest complete. See log file for details.") 