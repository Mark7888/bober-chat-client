from google.oauth2 import service_account
from google.auth.transport.requests import Request

import sys
import os

# get the first command line argument
if len(sys.argv) < 2:
    print("Usage: python generate.py <filename>")
    sys.exit(1)

# get the filename
filename = sys.argv[1]

# check if the file exists
if not os.path.exists(filename):
    print(f"Error: File '{filename}' not found")
    sys.exit(1)


SCOPES = ['https://www.googleapis.com/auth/cloud-platform', 'https://www.googleapis.com/auth/firebase']
SERVICE_ACCOUNT_FILE = filename

credentials = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE, scopes=SCOPES)


# Call the refresh method to obtain a new access token
credentials.refresh(Request())
if credentials.valid:
    print(credentials.token, end='')
    sys.exit(0)

print("Error: Unable to generate token")
sys.exit(1)
