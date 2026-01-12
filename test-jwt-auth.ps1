Write-Host "🚀 Testing JWT Authentication" -ForegroundColor Green
Write-Host "==============================" -ForegroundColor Green

$BASE_URL = "http://localhost:8080"

Write-Host ""
Write-Host "1. Testing User Registration..." -ForegroundColor Yellow
Write-Host "--------------------------------" -ForegroundColor Yellow

$registerBody = @{
    username = "testuser"
    email = "test@example.com"
    password = "password123"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/register" -Method POST -Body $registerBody -ContentType "application/json"
    Write-Host "Registration Response:" -ForegroundColor Cyan
    $registerResponse | ConvertTo-Json -Depth 3
    $ACCESS_TOKEN = $registerResponse.accessToken
} catch {
    Write-Host "Registration failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "2. Testing Login with Valid Credentials..." -ForegroundColor Yellow
Write-Host "------------------------------------------" -ForegroundColor Yellow

$loginBody = @{
    username = "testuser"
    password = "password123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    Write-Host "Login Response:" -ForegroundColor Cyan
    $loginResponse | ConvertTo-Json -Depth 3
    $ACCESS_TOKEN = $loginResponse.accessToken
} catch {
    Write-Host "Login failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "3. Testing Protected Endpoint with JWT Token..." -ForegroundColor Yellow
Write-Host "-----------------------------------------------" -ForegroundColor Yellow

try {
    $headers = @{
        "Authorization" = "Bearer $ACCESS_TOKEN"
    }
    $protectedResponse = Invoke-RestMethod -Uri "$BASE_URL/api/cars" -Method GET -Headers $headers
    Write-Host "Protected Endpoint Response:" -ForegroundColor Cyan
    $protectedResponse | ConvertTo-Json -Depth 3
} catch {
    Write-Host "Protected endpoint failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "4. Testing Login with Invalid Credentials..." -ForegroundColor Yellow
Write-Host "--------------------------------------------" -ForegroundColor Yellow

$invalidLoginBody = @{
    username = "testuser"
    password = "wrongpassword"
} | ConvertTo-Json

try {
    $invalidLoginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -Body $invalidLoginBody -ContentType "application/json"
    Write-Host "Invalid Login Response:" -ForegroundColor Cyan
    $invalidLoginResponse | ConvertTo-Json -Depth 3
} catch {
    Write-Host "Invalid login correctly failed: $($_.Exception.Message)" -ForegroundColor Green
}

Write-Host ""
Write-Host "5. Testing Protected Endpoint without Token..." -ForegroundColor Yellow
Write-Host "---------------------------------------------" -ForegroundColor Yellow

try {
    $noTokenResponse = Invoke-RestMethod -Uri "$BASE_URL/api/cars" -Method GET
    Write-Host "No Token Response:" -ForegroundColor Cyan
    $noTokenResponse | ConvertTo-Json -Depth 3
} catch {
    Write-Host "No token correctly failed: $($_.Exception.Message)" -ForegroundColor Green
}

Write-Host ""
Write-Host "✅ JWT Authentication Test Complete!" -ForegroundColor Green
