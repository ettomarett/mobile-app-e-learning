import os
from azure.ai.inference import ChatCompletionsClient
from azure.ai.inference.models import SystemMessage, UserMessage
from azure.core.credentials import AzureKeyCredential

# Configuration
AZURE_ENDPOINT = "https://DeepSeek-R1-gADK.eastus.models.ai.azure.com"
AZURE_API_KEY = "sczzACCarm4XtyfSQz5GQ3v5Hc2hSB2i"
AZURE_MODEL_NAME = "DeepSeek-R1"  # Fixed model name

def test_azure_sdk():
    print(f"Testing Azure SDK with model: {AZURE_MODEL_NAME}")
    print(f"Endpoint: {AZURE_ENDPOINT}")
    
    try:
        # Initialize client
        client = ChatCompletionsClient(
            endpoint=AZURE_ENDPOINT,
            credential=AzureKeyCredential(AZURE_API_KEY)
        )
        
        # Create messages
        messages = [
            SystemMessage(content="You are a helpful learning assistant for an e-learning app called SkillLearn."),
            UserMessage(content="Hello, can you help me learn Java?")
        ]
        
        # Send request
        print("Sending request to Azure OpenAI API...")
        response = client.complete(
            model=AZURE_MODEL_NAME,
            messages=messages,
            max_tokens=800,
            temperature=0.7
        )
        
        # Print response
        if response and response.choices:
            content = response.choices[0].message.content
            print("\nResponse from Azure OpenAI:")
            print(content)
        else:
            print("No response received.")
            
    except Exception as e:
        print(f"Error: {str(e)}")
        
if __name__ == "__main__":
    test_azure_sdk() 