#!/bin/bash

echo "🚀 Testing JWT Authentication"
echo "=============================="

BASE_URL="http://localhost:8080"

echo ""
echo "1. Testing User Registration..."
echo "--------------------------------"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }')

echo "Registration Response:"
echo "$REGISTER_RESPONSE" | jq '.'

ACCESS_TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.accessToken')

echo ""
echo "2. Testing Login with Valid Credentials..."
echo "------------------------------------------"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }')

echo "Login Response:"
echo "$LOGIN_RESPONSE" | jq '.'

ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken')

echo ""
echo "3. Testing Protected Endpoint with JWT Token..."
echo "-----------------------------------------------"
PROTECTED_RESPONSE=$(curl -s -X GET "$BASE_URL/api/cars" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "Protected Endpoint Response:"
echo "$PROTECTED_RESPONSE" | jq '.'

echo ""
echo "4. Testing Login with Invalid Credentials..."
echo "--------------------------------------------"
INVALID_LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "wrongpassword"
  }')

echo "Invalid Login Response:"
echo "$INVALID_LOGIN_RESPONSE" | jq '.'

echo ""
echo "5. Testing Protected Endpoint without Token..."
echo "---------------------------------------------"
NO_TOKEN_RESPONSE=$(curl -s -X GET "$BASE_URL/api/cars")

echo "No Token Response:"
echo "$NO_TOKEN_RESPONSE" | jq '.'

echo ""
echo "✅ JWT Authentication Test Complete!"
