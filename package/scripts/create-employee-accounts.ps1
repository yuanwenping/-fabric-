param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$AdminUsername = "platform_admin",
    [string]$AdminPassword = "Admin@15347361610",
    [string]$EmployeePassword = "12345678"
)

$ErrorActionPreference = "Stop"

function Invoke-JsonApi {
    param(
        [string]$Method,
        [string]$Uri,
        [string]$Token,
        [object]$Body
    )

    $parameters = @{
        Method = $Method
        Uri = $Uri
        ContentType = "application/json; charset=utf-8"
    }
    if ($Token) {
        $parameters.Headers = @{ Authorization = "Bearer $Token" }
    }
    if ($null -ne $Body) {
        $parameters.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 8))
    }
    Invoke-RestMethod @parameters
}

function Get-Text {
    param([int[]]$CodePoints)
    return -join ($CodePoints | ForEach-Object { [char]$_ })
}

$login = Invoke-JsonApi -Method Post -Uri "$BaseUrl/api/auth/login" -Body @{
    username = $AdminUsername
    password = $AdminPassword
}
$headersToken = $login.token

$organizations = Invoke-JsonApi -Method Get -Uri "$BaseUrl/api/admin/organizations" -Token $headersToken
$existingUsers = Invoke-JsonApi -Method Get -Uri "$BaseUrl/api/admin/users" -Token $headersToken
$existingNames = @{}
foreach ($user in $existingUsers) {
    $existingNames[[string]$user.username] = $true
}

$employees = @(
    @{ org = "SUP003"; phone = "13910000001"; name = (Get-Text 0x5F20,0x4E09); role = "ORG_MANAGER" },
    @{ org = "SUP003"; phone = "13910000002"; name = (Get-Text 0x674E,0x56DB); role = "SUPPLIER_BREEDER" },
    @{ org = "SUP003"; phone = "13910000003"; name = (Get-Text 0x738B,0x4E94); role = "SUPPLIER_QA" },
    @{ org = "SUP003"; phone = "13910000004"; name = (Get-Text 0x8D75,0x516D); role = "SUPPLIER_SHIPPER" },
    @{ org = "SUP003"; phone = "13910000005"; name = (Get-Text 0x9648,0x4E03); role = "SUPPLIER_OPERATOR" },
    @{ org = "PRO001"; phone = "13920000001"; name = (Get-Text 0x5218,0x6D0B); role = "ORG_MANAGER" },
    @{ org = "PRO001"; phone = "13920000002"; name = (Get-Text 0x5468,0x5F3A); role = "PROCESSOR_OPERATOR" },
    @{ org = "PRO001"; phone = "13920000003"; name = (Get-Text 0x5434,0x654F); role = "PROCESSOR_QA" },
    @{ org = "PRO001"; phone = "13920000004"; name = (Get-Text 0x90D1,0x4F1F); role = "PROCESSOR_WAREHOUSE" },
    @{ org = "LOG001"; phone = "13930000001"; name = (Get-Text 0x5B59,0x78CA); role = "ORG_MANAGER" },
    @{ org = "LOG001"; phone = "13930000002"; name = (Get-Text 0x4F55,0x9759); role = "LOGISTICS_DISPATCHER" },
    @{ org = "LOG001"; phone = "13930000003"; name = (Get-Text 0x6797,0x6D9B); role = "LOGISTICS_DRIVER" },
    @{ org = "RET001"; phone = "13940000001"; name = (Get-Text 0x5F90,0x4E3D); role = "ORG_MANAGER" },
    @{ org = "RET001"; phone = "13940000002"; name = (Get-Text 0x9A6C,0x8D85); role = "RETAIL_RECEIVER" },
    @{ org = "RET001"; phone = "13940000003"; name = (Get-Text 0x6731,0x7433); role = "RETAIL_SALESPERSON" },
    @{ org = "REG001"; phone = "13950000001"; name = (Get-Text 0x9AD8,0x5CF0); role = "ORG_MANAGER" },
    @{ org = "REG001"; phone = "13950000002"; name = (Get-Text 0x7F57,0x654F); role = "REGULATOR_INSPECTOR" }
)

$organizationByName = @{}
foreach ($organization in $organizations) {
    $organizationByName[[string]$organization.org_code] = $organization
}

$created = 0
$skipped = 0
foreach ($employee in $employees) {
    $organization = $organizationByName[$employee.org]
    if (-not $organization) {
        throw "Organization $($employee.org) was not found"
    }
    if ($existingNames.ContainsKey($employee.phone)) {
        Write-Host "Skip existing account: $($employee.phone) $($employee.name)"
        $skipped++
        continue
    }
    Invoke-JsonApi -Method Post -Uri "$BaseUrl/api/admin/users" -Token $headersToken -Body @{
        username = $employee.phone
        password = $EmployeePassword
        displayName = $employee.name
        orgId = $organization.id
        roleCode = $employee.role
    } | Out-Null
    Write-Host "Created account: $($employee.phone) $($employee.name) $($employee.role)"
    $existingNames[$employee.phone] = $true
    $created++
}

Write-Host "Employee account setup finished. Created=$created Skipped=$skipped"
