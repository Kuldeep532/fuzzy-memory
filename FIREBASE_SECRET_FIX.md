# 🔴 Google Services JSON Secret Fix

**आपका `GOOGLE_SERVICES_JSON` secret corrupt है।** यह permanently fix करने के लिए:

## समस्या
- Secret में stored JSON truncated या incomplete है
- `EOFException: End of input at line 32` error आ रहा है
- 100+ बार fail हो चुका है

## समाधान (5 मिनट में)

### Step 1: Firebase Console से नया google-services.json download करो
1. https://console.firebase.google.com पर जाओ
2. अपना project खोलो
3. **Project Settings** → **Your apps** → **Google-services.json** download करो
4. **पूरी फ़ाइल को नोटपैड में खोलो और verify करो** कि आखिरी में `}` है

### Step 2: Secret को बिल्कुल नया set करो

#### macOS/Linux:
```bash
cat ~/Downloads/google-services.json | pbcopy
```

#### Windows (PowerShell):
```powershell
Get-Content "C:\Users\YourName\Downloads\google-services.json" | Set-Clipboard
```

### Step 3: GitHub Secrets को update करो

1. https://github.com/Kuldeep532/fuzzy-memory/settings/secrets/actions पर जाओ
2. **`GOOGLE_SERVICES_JSON`** secret को खोलो
3. **पुरानी value को हटा दो** और **clipboard से paste करो**
4. **Save करो**

### Step 4: Verify करो

```bash
# Terminal में यह command चलाओ
cat google-services.json | python3 -m json.tool
```

अगर यह output दे तो ठीक है:
```json
{
  "type": "service_account",
  "project_id": "...",
  ...
}
```

## Extra Safety Check

```bash
# File size check करो (कम से कम 1KB होना चाहिए)
wc -c ~/Downloads/google-services.json
# Expected: 2000-5000 bytes
```

## अगर फिर भी fail हो

यह बताओ:
1. google-services.json file size क्या है?
2. Firebase में कितने apps configure हैं?
3. Workflow का complete error message क्या है?

---

**⚠️ IMPORTANT:** 
- कभी भी secret को `***` से store मत करो
- हमेशा complete JSON paste करो
- Last character `)` या `}` होना चाहिए
