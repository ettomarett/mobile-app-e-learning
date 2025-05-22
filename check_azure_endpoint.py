import requests
import json

# Configuration from the app
AZURE_RESOURCE_NAME = "DeepSeek-R1-gADK"
AZURE_ENDPOINT = f"https://{AZURE_RESOURCE_NAME}.eastus.models.ai.azure.com"
AZURE_API_KEY = "sczzACCarm4XtyfSQz5GQ3v5Hc2hSB2i"

def check_endpoint_info():
    """Basic check to see if the Azure endpoint exists"""
    print(f"Checking Azure OpenAI endpoint: {AZURE_ENDPOINT}")
    
    # Try with no authentication first to see if the endpoint exists
    try:
        response = requests.get(AZURE_ENDPOINT)
        print(f"Basic endpoint check (no auth): Status {response.status_code}")
        if response.status_code != 404:
            print(f"Response: {response.text[:200]}...")
    except Exception as e:
        print(f"Error connecting to endpoint: {str(e)}")
    
    # Try with authentication to check if credentials work
    headers = {"api-key": AZURE_API_KEY}
    try:
        response = requests.get(AZURE_ENDPOINT, headers=headers)
        print(f"Authenticated endpoint check: Status {response.status_code}")
        if response.status_code != 404:
            print(f"Response: {response.text[:200]}...")
    except Exception as e:
        print(f"Error with authenticated connection: {str(e)}")

def check_resource_exists():
    """Check if the Azure OpenAI resource exists using Azure Management API"""
    print("\nNOTE: This script can't verify your Azure resource without Azure Management API credentials.")
    print("To verify your Azure OpenAI resource:")
    print("1. Log into the Azure Portal (portal.azure.com)")
    print("2. Navigate to 'Azure OpenAI' service")
    print("3. Check if you have a resource named 'DeepSeek-R1-gADK'")
    print("4. Verify the region is 'eastus'")
    print("5. Check deployments to see if 'DeepSeek-R1' or similar is deployed")

def check_valid_account():
    """Check if the API key is valid for the account"""
    print("\nChecking if API key is valid by listing models...")
    
    # Try both common API versions
    api_versions = ["2023-05-15", "2024-05-01-preview"]
    
    for api_version in api_versions:
        url = f"{AZURE_ENDPOINT}/openai/models?api-version={api_version}"
        
        # Try both auth header styles
        auth_headers = [
            {"api-key": AZURE_API_KEY},
            {"Authorization": f"Bearer {AZURE_API_KEY}"}
        ]
        
        for headers in auth_headers:
            print(f"\nTrying to list models with API version {api_version}")
            print(f"Using headers: {headers}")
            
            try:
                response = requests.get(url, headers=headers)
                print(f"Status: {response.status_code}")
                
                if response.status_code == 200:
                    print("SUCCESS! API key is valid.")
                    data = response.json()
                    print("\nAvailable models:")
                    for model in data.get("data", []):
                        print(f"- {model.get('id', 'Unknown')}")
                    return True
                else:
                    print(f"Failed. Response: {response.text[:200]}...")
            except Exception as e:
                print(f"Error: {str(e)}")
    
    print("\nFailed to validate API key with all combinations.")
    return False

def suggest_fixes():
    """Suggest potential fixes for the connectivity issue"""
    print("\n============ POTENTIAL FIXES ============")
    print("1. Verify the resource name in Azure Portal")
    print("   - Current resource name: DeepSeek-R1-gADK")
    print("   - The resource name might be different from the model deployment name")
    
    print("\n2. Check API key validity")
    print("   - Verify the API key in Azure Portal > Azure OpenAI > Keys")
    print("   - Regenerate the API key if necessary")
    
    print("\n3. Check deployment names")
    print("   - In Azure Portal > Azure OpenAI > Deployments")
    print("   - Your deployment name might be different from 'DeepSeek-R1'")
    print("   - Look for any deployed models and use that exact name")
    
    print("\n4. Check region")
    print("   - Make sure your Azure OpenAI service is in 'eastus'")
    print("   - If not, update the endpoint URL in the app code")
    
    print("\n5. Check subscription permissions")
    print("   - Ensure your Azure subscription has access to Azure OpenAI")
    print("   - Some models require special access approval")

if __name__ == "__main__":
    print("=== Azure OpenAI Endpoint Configuration Checker ===\n")
    check_endpoint_info()
    check_resource_exists()
    valid_account = check_valid_account()
    
    if not valid_account:
        suggest_fixes()
    
    print("\nCheck complete. If issues persist, please review Azure Portal for correct configuration details.") 